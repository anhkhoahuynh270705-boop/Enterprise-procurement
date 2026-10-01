package com.example.shopping.common.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import com.example.shopping.common.dto.response.ApiErrorResponseDto;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import lombok.extern.slf4j.Slf4j;

/**
 * Global exception handler – xử lý tập trung tất cả exception và trả về
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ApiErrorResponseDto> handleDataConflict() {
        return buildError(HttpStatus.CONFLICT, "Dữ liệu bị trùng hoặc không còn hợp lệ. Vui lòng kiểm tra và thử lại.", null);
    }
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ApiErrorResponseDto> handleUnreadableRequest() {
        return buildError(HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ hoặc chứa trường không được phép cập nhật.", null);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiErrorResponseDto> handleUploadTooLarge() {
        return buildError(HttpStatus.PAYLOAD_TOO_LARGE, "File tối đa 10 MB.", null);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ApiErrorResponseDto> handleAccessDenied(AccessDeniedException ex) {
        return buildError(HttpStatus.FORBIDDEN, "Bạn không có quyền thực hiện thao tác này.", null);
    }

    /** Endpoint không tồn tại */
    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<ApiErrorResponseDto> handleNoResourceFound(NoResourceFoundException ex) {
        log.warn("No endpoint found: {}", ex.getMessage());
        return buildError(HttpStatus.NOT_FOUND, "Không tìm thấy endpoint: " + ex.getResourcePath(), null);
    }

    /** ResponseStatusException từ controller  */
    @ExceptionHandler(ResponseStatusException.class)
    public ResponseEntity<ApiErrorResponseDto> handleResponseStatus(ResponseStatusException ex) {
        String message = ex.getReason() != null && !ex.getReason().isBlank()
                ? ex.getReason()
                : (ex.getMessage() != null && !ex.getMessage().isBlank() ? ex.getMessage() : "Yêu cầu không thể xử lý");
        log.warn("ResponseStatusException [{}]: {}", ex.getStatusCode().value(), message);
        return buildError(HttpStatus.valueOf(ex.getStatusCode().value()), message, null);
    }

    /** Lỗi validation */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponseDto> handleValidationErrors(
            MethodArgumentNotValidException ex) {
        Map<String, String> fieldErrors = new HashMap<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            fieldErrors.put(error.getField(), error.getDefaultMessage());
        }
        return buildError(HttpStatus.BAD_REQUEST, "Dữ liệu không hợp lệ", fieldErrors);
    }

    /** HTTP method không được hỗ trợ */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ApiErrorResponseDto> handleMethodNotSupported(
            HttpRequestMethodNotSupportedException ex) {
        log.warn("Method not supported: {}", ex.getMessage());
        return buildError(HttpStatus.METHOD_NOT_ALLOWED, ex.getMessage(), null);
    }

    /** Lỗi nghiệp vụ (username tồn tại, sai mật khẩu…) */
    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiErrorResponseDto> handleIllegalArgument(
            IllegalArgumentException ex) {
        log.warn("Business error: {}", ex.getMessage());
        return buildError(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
    }

    /** Lỗi hệ thống (không kết nối được Keycloak) */
    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ApiErrorResponseDto> handleIllegalState(
            IllegalStateException ex) {
        log.error("System error: {}", ex.getMessage());
        return buildError(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), null);
    }

    /** Catch-all */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponseDto> handleGeneral(Exception ex) {
        log.error("Unhandled error: ", ex);
        return buildError(HttpStatus.INTERNAL_SERVER_ERROR,
                "Lỗi hệ thống, vui lòng thử lại sau.", null);
    }

    private ResponseEntity<ApiErrorResponseDto> buildError(
            HttpStatus status, String message, Object details) {
        var body = new ApiErrorResponseDto(LocalDateTime.now().toString(), status.value(), status.getReasonPhrase(), message, details);
        return ResponseEntity.status(status).body(body);
    }
}
