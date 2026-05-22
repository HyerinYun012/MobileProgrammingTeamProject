package com.petplace.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 1. 단순 소셜 로그인 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor // IDE 대입 경고 방지 및 Swagger 예시 바인딩용
@Schema(description = "소셜 로그인 요청 정보")
public class SocialLoginRequest {

    @Schema(description = "소셜 로그인 제공자", allowableValues = {"KAKAO", "NAVER"}, example = "KAKAO")
    @NotBlank(message = "소셜 프로바이더(PROVIDER)는 필수입니다.")
    private String provider;

    @Schema(description = "소셜 플랫폼(카카오/네이버)에서 발급받은 클라이언트 액세스 토큰", example = "AAAAO..._oauth_token_here_...")
    @NotBlank(message = "소셜 액세스 토큰은 필수입니다.")
    private String accessToken;
}