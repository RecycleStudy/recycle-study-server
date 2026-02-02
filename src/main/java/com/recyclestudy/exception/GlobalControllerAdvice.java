package com.recyclestudy.exception;

import com.recyclestudy.exception.dto.ErrorResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalControllerAdvice {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(final NotFoundException e) {
        log.warn("[NOT_FOUND] {}", e.getMessage());
        final ErrorResponse response = ErrorResponse.from(e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(response);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<ErrorResponse> handleBadRequest(final BadRequestException e) {
        log.warn("[BAD_REQUEST] {}", e.getMessage());
        final ErrorResponse response = ErrorResponse.from(e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(DeviceActivationExpiredException.class)
    public ResponseEntity<ErrorResponse> handleDeviceActivationExpired(final DeviceActivationExpiredException e) {
        log.warn("[DEVICE_ACTIVATION_EXPIRED] {}", e.getMessage());
        final ErrorResponse response = ErrorResponse.from(e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(UnauthorizedException.class)
    public ResponseEntity<ErrorResponse> handleUnauthorized(final UnauthorizedException e) {
        log.warn("[UNAUTHORIZED] {}", e.getMessage());
        final ErrorResponse response = ErrorResponse.from(e.getMessage());
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(response);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<ErrorResponse> handleForbidden(final ForbiddenException e) {
        log.warn("[FORBIDDEN] {}", e.getMessage());
        final ErrorResponse response = ErrorResponse.from(e.getMessage());
        return ResponseEntity.status(HttpStatus.FORBIDDEN).body(response);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgument(final IllegalArgumentException e) {
        log.warn("[ILLEGAL_ARGUMENT] {}", e.getMessage());
        final ErrorResponse response = ErrorResponse.from(e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingServletRequestParameter(
            final MissingServletRequestParameterException e) {
        log.warn("[MISSING_PARAMETER] {}", e.getMessage());
        final ErrorResponse response = ErrorResponse.from(e.getMessage());
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(final Exception e) {
        log.error("[INTERNAL_ERROR] 예기치 못한 에러 발생", e);
        final ErrorResponse response = ErrorResponse.from("예기치 못한 에러가 발생했습니다");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
    }
}
