package com.petplace.exception;

import lombok.AllArgsConstructor;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
@AllArgsConstructor
public enum ErrorCode {
    // 공통 에러
    USER_NOT_FOUND(HttpStatus.UNAUTHORIZED, "사용자를 찾을 수 없습니다."),
    POST_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 게시글입니다."),
    COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "존재하지 않는 댓글입니다."),
    PARENT_COMMENT_NOT_FOUND(HttpStatus.NOT_FOUND, "부모 댓글을 찾을 수 없습니다."),
    INVALID_PRICE_VALUE(HttpStatus.BAD_REQUEST, "가격은 음수일 수 없습니다."),

    DUPLICATE_BUSINESS_NUMBER(HttpStatus.CONFLICT, "이미 등록된 사업자 번호입니다."),

    // 403 Forbidden
    NO_PERMISSION(HttpStatus.FORBIDDEN, "권한이 없습니다."),

    // Bookmark 관련 에러
    RESTAURANT_NOT_FOUND(HttpStatus.NOT_FOUND, "장소를 찾을 수 없습니다."),

    // --- Auth 관련 에러 ---
    INVALID_LOGIN_INFO(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 일치하지 않습니다."),
    PASSWORD_NOT_MATCH(HttpStatus.BAD_REQUEST, "비밀번호 확인이 일치하지 않습니다."),
    DUPLICATE_LOGIN_ID(HttpStatus.CONFLICT, "이미 사용 중인 아이디입니다."),
    DUPLICATE_NICKNAME(HttpStatus.CONFLICT, "이미 사용 중인 닉네임입니다."),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "이미 가입된 이메일 주소입니다."),
    INVALID_PHONE_NUMBER(HttpStatus.BAD_REQUEST, "계정에 등록된 휴대폰 번호와 일치하지 않습니다."),
    UNSUPPORTED_SOCIAL_PROVIDER(HttpStatus.BAD_REQUEST, "지원하지 않는 소셜 로그인 제공자입니다."),
    SOCIAL_INFO_FETCH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "소셜 인증 정보를 불러올 수 없습니다."),
    KAKAO_AUTH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "카카오 인증에 실패했습니다."),
    NAVER_AUTH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "네이버 인증에 실패했습니다."),
    NAVER_INFO_FETCH_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "네이버 인증 정보 획득에 실패했습니다."),

    // Admin 전용 에러
    OWNER_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 사장님 정보를 찾을 수 없습니다."),
    REVIEW_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 리뷰를 찾을 수 없습니다."),
    ALREADY_REPORTED(HttpStatus.CONFLICT, "이미 신고한 리뷰입니다."),
    REVIEW_REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 신고 내역이 존재하지 않습니다."),
    COMMUNITY_REPORT_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 커뮤니티 신고 내역이 존재하지 않습니다."),

    // --- File 관련 에러 ---
    FILE_UPLOAD_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 업로드 중 오류가 발생했습니다."),
    FILE_DELETE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "파일 삭제 중 오류가 발생했습니다."),

    // --- Inquiry 관련 에러 ---
    INQUIRY_NOT_FOUND(HttpStatus.NOT_FOUND, "해당 문의 내역을 찾을 수 없습니다."),
    INQUIRY_SUBMISSION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "문의 등록 중 오류가 발생했습니다."),

    // --- Pet 관련 에러 ---
    PET_NOT_FOUND(HttpStatus.NOT_FOUND, "반려동물 정보를 찾을 수 없습니다."),

    // --- 식당 관련 에러 ---
    MENU_NOT_FOUND(HttpStatus.NOT_FOUND, "메뉴를 찾을 수 없습니다."),

    // Notice 관련 에러
    NOTICE_NOT_FOUND(HttpStatus.NOT_FOUND, "공지사항을 찾을 수 없습니다.");

    private final HttpStatus httpStatus;
    private final String message;
}