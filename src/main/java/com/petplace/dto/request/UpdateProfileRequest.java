package com.petplace.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

@Schema(description = "회원 프로필 수정 요청 객체")
public record UpdateProfileRequest(

        @Schema(description = "실명", example = "홍길동")
        String name,

        @NotBlank(message = "닉네임은 필수 입력 항목입니다.")
        @Size(min = 2, max = 10, message = "닉네임은 2자에서 10자 사이여야 합니다.")
        @Schema(description = "변경할 닉네임", example = "초코언니")
        String nickname,

        @Email(message = "올바른 이메일 형식이 아닙니다.")
        @Schema(description = "변경할 이메일 주소", example = "new_email@example.com")
        String email,

        @Pattern(regexp = "^$|^01(?:0|1|[6-9])(?:\\d{3}|\\d{4})\\d{4}$", message = "올바른 휴대폰 번호 형식이 아닙니다.")
        @Schema(description = "변경할 휴대폰 번호", example = "01099998888")
        String phone,

        @Size(min = 6, message = "비밀번호는 6자 이상이어야 합니다.")
        @Schema(description = "새 비밀번호 (변경 시에만 입력, 생략하면 기존 유지)", example = "newPass123", nullable = true)
        String newPassword
) {}
