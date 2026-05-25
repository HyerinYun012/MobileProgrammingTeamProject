package com.petplace.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "소셜 로그인 및 추가 정보 수집 요청 객체")
public class SocialLoginRequest {

    @Schema(description = "소셜 서비스 제공자", example = "KAKAO", allowableValues = {"KAKAO", "NAVER", "GOOGLE"})
    private String provider;

    @Schema(description = "소셜 서비스에서 제공하는 고유 식별자 (Subject)", example = "321456789")
    private String providerId;

    @Schema(description = "서비스 내에서 사용할 닉네임", example = "행복한집사")
    private String nickname;

    @Schema(description = "휴대폰 번호", example = "01012345678")
    private String phone;

    @Schema(description = "사용자 권한 (고객/사장님 구분)", example = "CUSTOMER", allowableValues = {"CUSTOMER", "OWNER"})
    private String role;

    @Schema(description = "마케팅 정보 수신 동의 여부", example = "true")
    private boolean marketingAgree;
}