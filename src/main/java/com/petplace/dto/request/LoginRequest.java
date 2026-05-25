package com.petplace.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "로그인 요청 객체")
public class LoginRequest {

    @Schema(description = "사용자 로그인 아이디", example = "petplace_user")
    private String loginId;

    @Schema(description = "사용자 비밀번호", example = "password123!")
    private String password;
}