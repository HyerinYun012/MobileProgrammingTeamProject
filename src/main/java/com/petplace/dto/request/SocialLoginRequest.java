package com.petplace.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "소셜 로그인 및 추가 정보 수집 요청 객체")
public class SocialLoginRequest {

    @NotBlank(message = "소셜 서비스 제공자(provider)는 필수입니다.")
    @Schema(description = "소셜 서비스 제공자", example = "KAKAO", allowableValues = {"KAKAO", "NAVER", "GOOGLE"})
    private String provider;

    // [추가] 소셜 사로부터 받은 엑세스 토큰 필드
    @NotBlank(message = "소셜 엑세스 토큰은 필수입니다.")
    @Schema(description = "소셜 서비스에서 발급받은 Access Token", example = "AAAA...token...BBBB")
    private String accessToken;

    @NotBlank(message = "소셜 고유 식별자(providerId)는 필수입니다.")
    @Schema(description = "소셜 서비스에서 제공하는 고유 식별자 (Subject)", example = "321456789")
    private String providerId;

    @NotBlank(message = "닉네임을 입력해주세요.")
    @Schema(description = "서비스 내에서 사용할 닉네임", example = "행복한집사")
    private String nickname;

    @NotBlank(message = "휴대폰 번호를 입력해주세요.")
    @Pattern(regexp = "^01(?:0|1|[6-9])(?:\\d{3}|\\d{4})\\d{4}$", message = "올바른 휴대폰 번호 형식이 아닙니다.")
    @Schema(description = "휴대폰 번호", example = "01012345678")
    private String phone;

    @NotBlank(message = "사용자 권한(role) 설정이 필요합니다.")
    @Schema(description = "사용자 권한 (고객/사장님 구분)", example = "CUSTOMER", allowableValues = {"CUSTOMER", "OWNER"})
    private String role;

    @Schema(description = "마케팅 정보 수신 동의 여부", example = "true")
    private boolean marketingAgree;
}