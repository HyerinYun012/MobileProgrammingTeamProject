package com.petplace.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "사장님(업체) 회원가입 요청 객체")
public class OwnerSignupRequest {

    @NotBlank(message = "아이디는 필수 입력 항목입니다.")
    @Size(min = 4, max = 20, message = "아이디는 4자에서 20자 사이여야 합니다.")
    @Schema(description = "로그인 아이디", example = "owner_petplace")
    private String loginId;

    @NotBlank(message = "비밀번호는 필수 입력 항목입니다.")
    @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다.")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$",
            message = "비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다.")
    @Schema(description = "비밀번호", example = "ownerpass123!")
    private String password;

    @NotBlank(message = "비밀번호 확인은 필수 입력 항목입니다.")
    private String passwordConfirm;

    @NotBlank(message = "성함은 필수 입력 항목입니다.")
    @Schema(description = "사장님 본명", example = "김주인")
    private String name;

    @NotBlank(message = "닉네임은 필수 입력 항목입니다.")
    @Size(min = 2, max = 15, message = "닉네임은 2자에서 15자 사이여야 합니다.")
    @Schema(description = "사장님 닉네임 (서비스 노출용)", example = "강아지천국점주")
    private String nickname;

    @NotBlank(message = "휴대폰 번호는 필수 입력 항목입니다.")
    @Pattern(regexp = "^01(?:0|1|[6-9])(?:\\d{3}|\\d{4})\\d{4}$", message = "올바른 휴대폰 번호 형식이어야 합니다.")
    @Schema(description = "사장님 개인 연락처 (- 없이 입력)", example = "01087654321")
    private String phone;

    // 🎯 [추가] 이메일 필드 및 검증 명세 도입
    @NotBlank(message = "이메일은 필수 입력 항목입니다.")
    @Email(message = "올바른 이메일 형식이어야 합니다.")
    @Schema(description = "사장님 이메일 주소 (정산 및 알림용)", example = "owner@example.com")
    private String email;
}