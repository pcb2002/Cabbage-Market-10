package com.example.cabbagemarket10.global.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    // Common
    VALIDATION_ERROR(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "요청값 검증에 실패했습니다."),
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "INVALID_INPUT", "잘못된 요청입니다."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "인증이 필요합니다."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "접근 권한이 없습니다."),
    NOT_FOUND(HttpStatus.NOT_FOUND, "NOT_FOUND", "요청한 자원을 찾을 수 없습니다."),
    METHOD_NOT_ALLOWED(HttpStatus.METHOD_NOT_ALLOWED, "METHOD_NOT_ALLOWED", "지원하지 않는 HTTP 메서드입니다."),
    INTERNAL_SERVER_ERROR(
            HttpStatus.INTERNAL_SERVER_ERROR,
            "INTERNAL_SERVER_ERROR",
            "서버 내부 오류가 발생했습니다."),

    // Auth
    DUPLICATED_EMAIL(HttpStatus.CONFLICT, "DUPLICATED_EMAIL", "이미 사용 중인 이메일입니다."),
    LOGIN_FAILED(HttpStatus.UNAUTHORIZED, "LOGIN_FAILED", "이메일 또는 비밀번호가 올바르지 않습니다."),
    SUSPENDED_ACCOUNT(HttpStatus.FORBIDDEN,"SUSPENDED_ACCOUNT", "정지된 회원은 로그인할 수 없습니다."),
    INVALID_REFRESH_TOKEN(
            HttpStatus.UNAUTHORIZED,
            "INVALID_REFRESH_TOKEN",
            "유효하지 않은 Refresh Token입니다."),
    REFRESH_TOKEN_EXPIRED(
            HttpStatus.UNAUTHORIZED,
            "REFRESH_TOKEN_EXPIRED",
            "Refresh Token이 만료되었습니다."),
    BLACKLISTED_TOKEN(
            HttpStatus.UNAUTHORIZED,
            "BLACKLISTED_TOKEN",
            "로그아웃되었거나 폐기된 토큰입니다."),
    REFRESH_TOKEN_NOT_FOUND(HttpStatus.NOT_FOUND,
            "REFRESH_TOKEN_NOT_FOUND",
            "저장된 Refresh Token을 찾을 수 없습니다."),
    ACCESS_TOKEN_MISSING(
            HttpStatus.UNAUTHORIZED,
            "ACCESS_TOKEN_MISSING",
            "Access Token이 필요합니다."),

    ACCESS_TOKEN_INVALID(
            HttpStatus.UNAUTHORIZED,
            "ACCESS_TOKEN_INVALID",
            "유효하지 않은 Access Token입니다."),

    ACCESS_TOKEN_EXPIRED(
            HttpStatus.UNAUTHORIZED,
            "ACCESS_TOKEN_EXPIRED",
            "Access Token이 만료되었습니다."),


    // category
    CATEGORY_NOT_FOUND(HttpStatus.NOT_FOUND, "CATEGORY_NOT_FOUND", "카테고리를 찾을 수 없습니다."),

    // item
    ITEM_NOT_FOUND(HttpStatus.NOT_FOUND, "ITEM_NOT_FOUND", "요청한 ID의 상품을 찾을 수 없습니다."),
    ITEM_UPDATE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "ITEM_UPDATE_NOT_ALLOWED", "등록된 경매 상품은 수정할 수 없습니다."),
    ITEM_STATUS_UPDATE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "ITEM_STATUS_UPDATE_NOT_ALLOWED", "임시저장 상품의 판매 상태는 변경할 수 없습니다."),
    ITEM_PUBLISH_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "ITEM_PUBLISH_NOT_ALLOWED", "이미 게시된 상품입니다."),

    // Auction
    AUCTION_STATUS_NOT_FOUND(HttpStatus.NOT_FOUND, "AUCTION_STATUS_NOT_FOUND","경매 상태 정보를 찾을 수 없습니다."),
    AUCTION_ALREADY_CLOSED(HttpStatus.BAD_REQUEST, "AUCTION_ALREADY_CLOSED", "이미 마감된 경매입니다."),
    INVALID_AUCTION_CLOSE_DATE(HttpStatus.BAD_REQUEST, "INVALID_AUCTION_CLOSE_DATE", "경매 종료일은 현재 시각 이후로 설정해야 합니다."),
    INVALID_BID_PRICE(HttpStatus.BAD_REQUEST, "INVALID_BID_PRICE", "입찰가는 현재 최고 입찰가보다 높아야 합니다."),
    AUCTION_ALREADY_IN_PROGRESS(HttpStatus.BAD_REQUEST, "AUCTION_ALREADY_IN_PROGRESS","이미 입찰자가 존재하여 수정할 수 없습니다."),

    // Inquiry
    // 존재하지 않는 문의 조회 또는 수정 요청
    INQUIRY_NOT_FOUND(HttpStatus.NOT_FOUND, "INQUIRY_NOT_FOUND", "문의를 찾을 수 없습니다."),
    // 이미 답변이 달린 문의에 중복 답변 요청
    ANSWER_ALREADY_EXISTS(HttpStatus.CONFLICT, "ANSWER_ALREADY_EXISTS", "이미 답변이 존재합니다."),

    // Review
    // 리뷰 대상 회원 또는 조회 회원 없음
    CLIENT_NOT_FOUND(HttpStatus.NOT_FOUND, "CLIENT_NOT_FOUND", "회원을 찾을 수 없습니다."),
    REVIEW_NOT_ALLOWED(HttpStatus.FORBIDDEN, "REVIEW_NOT_ALLOWED", "리뷰를 작성할 수 없는 사용자입니다."),
    REVIEW_ITEM_NOT_COMPLETED(HttpStatus.BAD_REQUEST, "REVIEW_ITEM_NOT_COMPLETED", "거래 완료된 상품에만 리뷰를 작성할 수 있습니다."),
    REVIEW_ALREADY_EXISTS(HttpStatus.CONFLICT, "REVIEW_ALREADY_EXISTS", "이미 작성한 리뷰가 있습니다."),
    SELF_REVIEW_NOT_ALLOWED(HttpStatus.FORBIDDEN, "SELF_REVIEW_NOT_ALLOWED", "본인이 판매한 상품에는 리뷰를 작성할 수 없습니다."),
    // 존재하지 않는 리뷰 조회 또는 수정 요청
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "REVIEW_NOT_FOUND", "리뷰를 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String code;
    private final String message;

    ErrorCode(HttpStatus httpStatus, String code, String message) {
        this.httpStatus = httpStatus;
        this.code = code;
        this.message = message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
