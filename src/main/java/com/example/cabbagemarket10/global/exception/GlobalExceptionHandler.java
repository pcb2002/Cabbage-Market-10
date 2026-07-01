package com.example.cabbagemarket10.global.exception;

import com.example.cabbagemarket10.global.common.CommonResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.Objects;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;
import tools.jackson.databind.exc.InvalidFormatException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final String CLIENT_EMAIL_UNIQUE_CONSTRAINT = "uk_client_email";

    // 서비스 계층에서 정의한 비즈니스 예외를 공통 오류 응답으로 변환한다.
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<CommonResponse<Void>> handleBusinessException(BusinessException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        return CommonResponse.fail(errorCode, exception.getMessage())
                .toResponseEntity();
    }

    // 동시 가입 등으로 이메일 유니크 제약을 직접 위반한 경우를 중복 이메일로 변환한다(레이스 안전망).
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<CommonResponse<Void>> handleDataIntegrityViolation(
            DataIntegrityViolationException exception) {
        if (isClientEmailUniqueConstraintViolation(exception)) {
            log.warn("이메일 유니크 제약 위반을 중복 이메일로 처리", exception);
            return CommonResponse.fail(ErrorCode.DUPLICATED_EMAIL)
                    .toResponseEntity();
        }

        log.warn("데이터 무결성 위반 발생", exception);
        return CommonResponse.fail(ErrorCode.INVALID_INPUT)
                .toResponseEntity();
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<CommonResponse<Void>> handleMethodArgumentNotValidException(
            MethodArgumentNotValidException exception) {
        return validationErrorResponse(extractFirstErrorMessage(exception));
    }

    @ExceptionHandler(BindException.class)
    public ResponseEntity<CommonResponse<Void>> handleBindException(BindException exception) {
        return validationErrorResponse(extractFirstErrorMessage(exception));
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<CommonResponse<Void>> handleConstraintViolationException(
            ConstraintViolationException exception) {
        String message = exception.getConstraintViolations().stream()
                .findFirst()
                .map(violation -> violation.getMessage())
                .orElse(ErrorCode.VALIDATION_ERROR.getMessage());
        return validationErrorResponse(message);
    }

    @ExceptionHandler({
            MissingServletRequestParameterException.class,
            MethodArgumentTypeMismatchException.class,
            HttpMessageNotReadableException.class
    })
    public ResponseEntity<CommonResponse<Void>> handleBadRequestException(Exception exception) {
        if (exception instanceof HttpMessageNotReadableException && isEnumDeserializationError(exception)) {
            return CommonResponse.fail(ErrorCode.INVALID_INPUT)
                    .toResponseEntity();
        }
        return validationErrorResponse(ErrorCode.VALIDATION_ERROR.getMessage());
    }

    @ExceptionHandler({
            NoHandlerFoundException.class,
            NoResourceFoundException.class
    })
    public ResponseEntity<CommonResponse<Void>> handleNotFoundException(Exception exception) {
        return CommonResponse.fail(ErrorCode.NOT_FOUND)
                .toResponseEntity();
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<CommonResponse<Void>> handleMethodNotAllowedException(
            HttpRequestMethodNotSupportedException exception) {
        return CommonResponse.fail(ErrorCode.METHOD_NOT_ALLOWED)
                .toResponseEntity();
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonResponse<Void>> handleException(Exception exception) {
        log.error("처리되지 않은 예외 발생", exception);
        return CommonResponse.fail(ErrorCode.INTERNAL_SERVER_ERROR)
                .toResponseEntity();
    }

    // Bean Validation 실패 응답 형식을 한 곳에서 맞춘다.
    private ResponseEntity<CommonResponse<Void>> validationErrorResponse(String message) {
        return CommonResponse.fail(ErrorCode.VALIDATION_ERROR, message)
                .toResponseEntity();
    }

    // 여러 필드 오류가 있어도 첫 번째 메시지만 공통 응답에 사용한다.
    private String extractFirstErrorMessage(BindException exception) {
        return exception.getBindingResult().getFieldErrors().stream()
                .map(this::resolveFieldErrorMessage)
                .findFirst()
                .orElse(ErrorCode.VALIDATION_ERROR.getMessage());
    }

    private String resolveFieldErrorMessage(FieldError error) {
        // 타입 변환 실패(enum 등 바인딩 실패)는 Spring 기본 영문 메시지 대신 ErrorCode 메시지로 통일한다.
        if (error.isBindingFailure()) {
            return ErrorCode.INVALID_INPUT.getMessage();
        }
        // @Size, @PositiveOrZero 등 검증 실패는 지정한 커스텀 메시지를 그대로 사용한다.
        return Objects.requireNonNullElse(
                error.getDefaultMessage(),
                ErrorCode.VALIDATION_ERROR.getMessage());
    }

    private boolean isEnumDeserializationError(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof InvalidFormatException invalidFormatException
                    && invalidFormatException.getTargetType() != null
                    && invalidFormatException.getTargetType().isEnum()) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean isClientEmailUniqueConstraintViolation(Throwable exception) {
        Throwable current = exception;
        while (current != null) {
            if (current instanceof org.hibernate.exception.ConstraintViolationException constraintException
                    && CLIENT_EMAIL_UNIQUE_CONSTRAINT.equalsIgnoreCase(constraintException.getConstraintName())) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}
