package com.example.cabbagemarket10.global.exception;

import com.example.cabbagemarket10.global.common.CommonResponse;
import jakarta.validation.ConstraintViolationException;
import java.util.Objects;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    // 서비스 계층에서 정의한 비즈니스 예외를 공통 오류 응답으로 변환한다.
    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<CommonResponse<Void>> handleBusinessException(BusinessException exception) {
        ErrorCode errorCode = exception.getErrorCode();
        return CommonResponse.fail(errorCode, exception.getMessage())
                .toResponseEntity(errorCode.getHttpStatus());
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
        return validationErrorResponse(ErrorCode.VALIDATION_ERROR.getMessage());
    }

    @ExceptionHandler({
            NoHandlerFoundException.class,
            NoResourceFoundException.class
    })
    public ResponseEntity<CommonResponse<Void>> handleNotFoundException(Exception exception) {
        return CommonResponse.fail(ErrorCode.NOT_FOUND)
                .toResponseEntity(ErrorCode.NOT_FOUND.getHttpStatus());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<CommonResponse<Void>> handleMethodNotAllowedException(
            HttpRequestMethodNotSupportedException exception) {
        return CommonResponse.fail(ErrorCode.METHOD_NOT_ALLOWED)
                .toResponseEntity(ErrorCode.METHOD_NOT_ALLOWED.getHttpStatus());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<CommonResponse<Void>> handleException(Exception exception) {
        return CommonResponse.fail(ErrorCode.INTERNAL_SERVER_ERROR)
                .toResponseEntity(ErrorCode.INTERNAL_SERVER_ERROR.getHttpStatus());
    }

    // Bean Validation 실패 응답 형식을 한 곳에서 맞춘다.
    private ResponseEntity<CommonResponse<Void>> validationErrorResponse(String message) {
        return CommonResponse.fail(ErrorCode.VALIDATION_ERROR, message)
                .toResponseEntity(ErrorCode.VALIDATION_ERROR.getHttpStatus());
    }

    // 여러 필드 오류가 있어도 첫 번째 메시지만 공통 응답에 사용한다.
    private String extractFirstErrorMessage(BindException exception) {
        return exception.getBindingResult().getFieldErrors().stream()
                .map(error -> Objects.requireNonNullElse(
                        error.getDefaultMessage(),
                        ErrorCode.VALIDATION_ERROR.getMessage()))
                .findFirst()
                .orElse(ErrorCode.VALIDATION_ERROR.getMessage());
    }
}
