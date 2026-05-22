package com.petplace.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 2. 신규 소셜 회원가입 요청 DTO (추가 정보 입력 단계)
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "신규 소셜 회원 추가 정보 입력 및 가입 요청 정보")
public class SocialSignupRequest {

    @Schema(description = "소셜 로그인 제공자", allowableValues = {"KAKAO", "NAVER"}, example = "KAKAO")
    @NotBlank(message = "소셜 프로바이더(PROVIDER)는 필수입니다.")
    private String provider;

    @Schema(description = "소셜 플랫폼(카카오/네이버)에서 발급받은 클라이언트 액세스 토큰", example = "AAAAO..._oauth_token_here_...")
    @NotBlank(message = "소셜 액세스 토큰은 필수입니다.")
    private String accessToken;

    @Schema(description = "서비스 내에서 사용할 고유 닉네임", example = "초코누나")
    @NotBlank(message = "닉네임을 입력해주세요.")
    private String nickname;

    @Schema(description = "사용자 전화번호 (하이픈 제외 숫자만 권장)", example = "01012345678")
    @NotBlank(message = "휴대폰 번호를 입력해주세요.")
    private String phone;

    @Schema(description = "사용자 서비스 권한", allowableValues = {"CUSTOMER", "OWNER"}, example = "CUSTOMER")
    @NotBlank(message = "사용자 권한(role) 설정이 필요합니다.")
    private String role;

    @Schema(description = "마케팅 정보 수신 동의 여부", example = "true")
    private boolean marketingAgree;
}