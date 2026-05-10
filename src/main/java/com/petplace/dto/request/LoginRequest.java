package com.petplace.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
@Schema(description = "로그인 요청 객체")
public class LoginRequest {

    @NotBlank(message = "아이디를 입력해주세요.")
    @Schema(description = "사용자 로그인 아이디", example = "petplace_user")
    private String loginId;

    @NotBlank(message = "비밀번호를 입력해주세요.")
    @Schema(description = "사용자 비밀번호", example = "password123!")
    private String password;
}