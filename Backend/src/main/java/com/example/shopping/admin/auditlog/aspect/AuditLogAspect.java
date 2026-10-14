package com.example.shopping.admin.auditlog.aspect;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.atomic.AtomicLong;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.example.shopping.admin.auditlog.annotation.Audited;
import com.example.shopping.admin.auditlog.entity.AuditLogEntry;
import com.example.shopping.admin.auditlog.service.AuditLogService;

import jakarta.servlet.http.HttpServletRequest;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

/**
 * Intercepts methods annotated with {@link Audited} and persists an AuditLogEntry.
 * Runs in a try/catch so that a logging failure never breaks the actual business operation.
 */
@Aspect
@Component
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
@RequiredArgsConstructor
public class AuditLogAspect {

    AuditLogService auditLogService;

    private static final AtomicLong COUNTER = new AtomicLong(0);
    private static final DateTimeFormatter CODE_FMT =
            DateTimeFormatter.ofPattern("yyyyMMdd");

    @Around("@annotation(audited)")
    public Object audit(ProceedingJoinPoint pjp, Audited audited) throws Throwable {
        long start = System.currentTimeMillis();
        LocalDateTime now = LocalDateTime.now();

        String username = resolveUsername();
        String userFullName = resolveFullName();
        String userAgent = resolveUserAgent();
        String method = resolveHttpMethod();
        String logCode = "LOG-" + CODE_FMT.format(now) + "-" + COUNTER.incrementAndGet();

        Object result = null;
        String status = "SUCCESS";
        String description = audited.description();

        try {
            result = pjp.proceed();
        } catch (Throwable t) {
            status = "FAILED";
            description = description + " — Lỗi: " + t.getMessage();
            throw t;
        } finally {
            int elapsed = (int) (System.currentTimeMillis() - start);
            try {
                AuditLogEntry entry = AuditLogEntry.builder()
                        .logCode(logCode)
                        .occurredAt(now)
                        .username(username)
                        .userFullName(userFullName)
                        .action(audited.action())
                        .module(audited.module())
                        .moduleLabel(audited.moduleLabel())
                        .status(status)
                        .description(description)
                        .userAgent(userAgent)
                        .httpMethod(method)
                        .responseTimeMs(elapsed)
                        .build();
                auditLogService.record(entry);
            } catch (Exception ex) {
                log.warn("Failed to persist audit entry:", ex.getMessage());
            }
        }
        return result;
    }

    private String resolveUsername() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth instanceof JwtAuthenticationToken jwt) {
                String u = jwt.getToken().getClaimAsString("preferred_username");
                if (u != null && !u.isBlank()) return u;
            }
            return auth != null ? auth.getName() : "anonymous";
        } catch (Exception e) {
            return "anonymous";
        }
    }

    private String resolveFullName() {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth instanceof JwtAuthenticationToken jwt) {
                String name = jwt.getToken().getClaimAsString("name");
                if (name != null && !name.isBlank()) return name;
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private String resolveUserAgent() {
        return getRequest() == null ? null : getRequest().getHeader("User-Agent");
    }

    private String resolveHttpMethod() {
        return getRequest() == null ? null : getRequest().getMethod();
    }

    private HttpServletRequest getRequest() {
        try {
            return ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes())
                    .getRequest();
        } catch (Exception e) {
            return null;
        }
    }
}
