package com.petplace.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "비밀번호 재설정 요청 객체")
public class ResetPasswordRequest {

    @NotBlank(message = "아이디는 필수 입력 항목입니다.")
    @Schema(description = "로그인 아이디", example = "petplace123")
    private String loginId;

    @NotBlank(message = "휴대폰 번호는 필수 입력 항목입니다.")
    @Pattern(regexp = "^01(?:0|1|[6-9])(?:\\d{3}|\\d{4})\\d{4}$", message = "올바른 휴대폰 번호 형식이 아닙니다.")
    @Schema(description = "휴대폰 번호", example = "01012345678")
    private String phone;

    @NotBlank(message = "새 비밀번호는 필수 입력 항목입니다.")
    @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다.")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$",
            message = "비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다.")
    @Schema(description = "변경할 새 비밀번호", example = "newpass123!")
    private String newPassword;
}