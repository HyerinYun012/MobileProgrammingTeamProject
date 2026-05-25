package com.petplace.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "일반 고객 회원가입 요청 객체")
public class CustomerSignupRequest {

    @Schema(description = "사용자 실명", example = "홍길동")
    private String name;

    @Schema(description = "로그인 아이디", example = "petlover123")
    private String loginId;

    @Schema(description = "비밀번호 (8자 이상, 특수문자 포함 권장)", example = "password123!")
    private String password;

    @Schema(description = "비밀번호 확인 (password 필드와 동일해야 함)", example = "password123!")
    private String passwordConfirm;

    @Schema(description = "서비스 내에서 사용할 닉네임", example = "초코아빠")
    private String nickname;

    @Schema(description = "휴대폰 번호 (하이픈 제외)", example = "01012345678")
    private String phone;
}