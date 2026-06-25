package com.example.cabbagemarket10.global.common;

import com.example.cabbagemarket10.global.exception.ErrorCode;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonPropertyOrder;
import lombok.Getter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

// 모든 API의 공통 응답 래퍼.
@Getter
@JsonPropertyOrder({"status", "code", "message", "data"})
public class CommonResponse<T> {

    private final int status;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String code;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String message;

    private final T data;

    /**
     * 공통 응답 객체를 생성한다.
     */
    private CommonResponse(int status, String code, String message, T data) {
        this.status = status;
        this.code = code;
        this.message = message;
        this.data = data;
    }

    /**
     * 응답 본문 데이터 없이 성공 응답 객체를 생성한다.
     */
    public static CommonResponse<Void> success(HttpStatus httpStatus) {
        return new CommonResponse<>(httpStatus.value(), null, null, null);
    }

    /**
     * 응답 본문 데이터를 포함한 성공 응답 객체를 생성한다.
     */
    public static <T> CommonResponse<T> success(HttpStatus httpStatus, T data) {
        return new CommonResponse<>(httpStatus.value(), null, null, data);
    }

    /**
     * ErrorCode의 기본 메시지를 사용해 실패 응답 객체를 생성한다.
     */
    public static CommonResponse<Void> fail(ErrorCode errorCode) {
        return fail(errorCode, errorCode.getMessage());
    }

    /**
     * ErrorCode와 사용자 정의 메시지로 실패 응답 객체를 생성한다.
     */
    public static CommonResponse<Void> fail(ErrorCode errorCode, String message) {
        return new CommonResponse<>(errorCode.getHttpStatus().value(), errorCode.getCode(), message, null);
    }

    /**
     * 현재 공통 응답 객체를 자신의 status로 ResponseEntity에 감싸 반환한다.
     */
    public ResponseEntity<CommonResponse<T>> toResponseEntity() {
        return ResponseEntity.status(status).body(this);
    }

    /**
     * 추가 응답 헤더와 함께 ResponseEntity로 감싸 반환한다.
     */
    public ResponseEntity<CommonResponse<T>> toResponseEntity(HttpHeaders headers) {
        return ResponseEntity.status(status).headers(headers).body(this);
    }
}
