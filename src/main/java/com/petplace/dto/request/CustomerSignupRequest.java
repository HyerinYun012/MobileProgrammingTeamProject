package com.petplace.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
@Schema(description = "일반 고객 회원가입 요청 객체")
public class CustomerSignupRequest {

    @NotBlank(message = "이름은 필수 입력 항목입니다.")
    @Schema(description = "사용자 실명", example = "홍길동")
    private String name;

    @NotBlank(message = "아이디는 필수 입력 항목입니다.")
    @Size(min = 4, max = 20, message = "아이디는 4자에서 20자 사이여야 합니다.")
    @Schema(description = "로그인 아이디", example = "petlover123")
    private String loginId;

    @NotBlank(message = "비밀번호는 필수 입력 항목입니다.")
    @Size(min = 8, message = "비밀번호는 최소 8자 이상이어야 합니다.")
    @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d)(?=.*[@$!%*#?&])[A-Za-z\\d@$!%*#?&]{8,}$",
            message = "비밀번호는 영문, 숫자, 특수문자를 포함해야 합니다.")
    @Schema(description = "비밀번호 (8자 이상, 특수문자 포함 권장)", example = "password123!")
    private String password;

    @NotBlank(message = "비밀번호 확인은 필수 입력 항목입니다.")
    private String passwordConfirm;

    @NotBlank(message = "닉네임은 필수 입력 항목입니다.")
    @Size(min = 2, max = 15, message = "닉네임은 2자에서 15자 사이여야 합니다.")
    @Schema(description = "서비스 활동 닉네임", example = "초코언니")
    private String nickname;

    @NotBlank(message = "휴대폰 번호는 필수 입력 항목입니다.")
    @Pattern(regexp = "^01(?:0|1|[6-9])(?:\\d{3}|\\d{4})\\d{4}$", message = "올바른 휴대폰 번호 형식이어야 합니다.")
    @Schema(description = "휴대폰 번호 (- 없이 입력)", example = "01012345678")
    private String phone;

    // 🎯 [추가] 이메일 필드 및 검증 명세 도입
    @NotBlank(message = "이메일은 필수 입력 항목입니다.")
    @Email(message = "올바른 이메일 형식이어야 합니다.")
    @Schema(description = "사용자 이메일 주소", example = "customer@example.com")
    private String email;
}