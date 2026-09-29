package com.example.shopping.security;

import com.example.shopping.procurement.entity.ProcurementTicket;
import com.example.shopping.procurement.enums.ProcurementStatus;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

public final class TicketAccess {
    private TicketAccess() { }

    public static boolean hasRole(String role) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && auth.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_" + role));
    }

    public static String username() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !auth.isAuthenticated()) 
            throw denied();
        if (auth instanceof JwtAuthenticationToken jwt) {
            String username = jwt.getToken().getClaimAsString("preferred_username");
            if (username != null && !username.isBlank()) 
                return username;
        }
        return auth.getName();
    }

    public static void requireReader() {
        if (!hasRole("ADMIN") && !hasRole("USER") && !hasRole("CHECKER")) 
            throw denied();
    }

    public static void requireRead(ProcurementTicket ticket) {
        if (hasRole("ADMIN")) 
            return;
        if (hasRole("CHECKER")) {
            if (ticket.getStatus() == ProcurementStatus.PENDING_APPROVAL) 
                return;
        } else if (hasRole("USER") && username().equals(ticket.getMakerUsername())) 
            return;
        throw denied();
    }

    public static void requireSubmit(ProcurementTicket ticket) {
        if (hasRole("ADMIN")) 
            return;
        if (!hasRole("USER") || !username().equals(ticket.getMakerUsername())) 
            throw denied();
    }

    private static AccessDeniedException denied() {
        return new AccessDeniedException("Bạn không có quyền truy cập phiếu này.");
    }
}
