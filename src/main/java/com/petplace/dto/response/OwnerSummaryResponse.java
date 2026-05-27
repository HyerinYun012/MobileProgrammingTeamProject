package com.petplace.dto.response;

import com.petplace.entity.User;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "가입 승인 대기 사장님 요약 정보 응답")
public class OwnerSummaryResponse {

    @Schema(description = "사장님 유저 ID", example = "1")
    private Long ownerId;

    @Schema(description = "로그인 아이디", example = "owner_petplace")
    private String loginId;

    @Schema(description = "사장님 실명", example = "김사장")
    private String name;

    @Schema(description = "이메일 주소", example = "owner@petplace.com")
    private String email;

    @Schema(description = "연락처", example = "010-1234-5678")
    private String phone;

    @Schema(description = "가입 신청 일시")
    private LocalDateTime createdAt;

    public static OwnerSummaryResponse from(User user) {
        return OwnerSummaryResponse.builder()
                .ownerId(user.getId())
                .loginId(user.getLocalAuth() != null ? user.getLocalAuth().getLoginId() : "소셜 계정")
                .name(user.getName() != null ? user.getName() : user.getNickname())
                .email(user.getEmail())
                .phone(user.getPhone())
                .createdAt(user.getCreatedAt())
                .build();
    }
}