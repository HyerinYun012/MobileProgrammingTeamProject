package com.petplace.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "마이페이지 사용자 프로필 응답 정보")
public record UserProfileResponse(
        @Schema(description = "사용자 고유 ID", example = "1")
        Long id,

        @Schema(description = "사용자 이메일", example = "user@petplace.com")
        String email,

        @Schema(description = "사용자 닉네임", example = "초코언니")
        String nickname,

        @Schema(description = "프로필 이미지 URL", example = "/images/profiles/choco.png", nullable = true)
        String profileImageUrl
) {}