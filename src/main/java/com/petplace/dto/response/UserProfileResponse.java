package com.petplace.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "마이페이지 사용자 프로필 응답 정보")
public record UserProfileResponse(
        @Schema(description = "사용자 고유 ID", example = "1")
        Long id,

        @Schema(description = "사용자 실명", example = "홍길동")
        String name,

        @Schema(description = "사용자 닉네임", example = "초코언니")
        String nickname,

        @Schema(description = "로그인 아이디 (변경 불가)", example = "user123")
        String loginId,

        @Schema(description = "사용자 이메일", example = "user@petplace.com")
        String email,

        @Schema(description = "휴대폰 번호", example = "01012345678")
        String phone,

        @Schema(description = "프로필 이미지 URL", example = "/images/profiles/choco.png", nullable = true)
        String profileImageUrl,

        @Schema(description = "사용자 역할 (CUSTOMER / OWNER / ADMIN)", example = "CUSTOMER")
        String role
) {}
