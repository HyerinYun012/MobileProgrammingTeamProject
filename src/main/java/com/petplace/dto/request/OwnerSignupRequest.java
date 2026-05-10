package com.petplace.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.AssertTrue;
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

    // [추가] 비밀번호 일치 여부 검증
    @AssertTrue(message = "비밀번호가 일치하지 않습니다.")
    public boolean isPasswordMatching() {
        if (this.password == null || this.passwordConfirm == null) return false;
        return this.password.equals(this.passwordConfirm);
    }

    // [추가] 유저 엔티티와 AuthService의 로직을 위해 성함 필드 추가
    @NotBlank(message = "성함은 필수 입력 항목입니다.")
    @Schema(description = "사장님 본명", example = "김주인")
    private String name;

    @NotBlank(message = "닉네임은 필수 입력 항목입니다.")
    @Size(min = 2, max = 15, message = "닉네임은 2자에서 15자 사이여야 합니다.")
    @Schema(description = "사장님 닉네임 (서비스 노출용)", example = "강아지천국점주")
    private String nickname;

    @NotBlank(message = "휴대폰 번호는 필수 입력 항목입니다.")
    @Pattern(regexp = "^01(?:0|1|[6-9])(?:\\d{3}|\\d{4})\\d{4}$", message = "올바른 휴대폰 번호 형식이 아닙니다.")
    @Schema(description = "휴대폰 번호", example = "01098765432")
    private String phone;

    @NotBlank(message = "사업자 등록 번호는 필수 입력 항목입니다.")
    @Pattern(regexp = "^\\d{3}-\\d{2}-\\d{5}$", message = "사업자 번호 형식(000-00-00000)을 확인해주세요.")
    @Schema(description = "사업자 등록 번호", example = "123-45-67890")
    private String businessNo;

    @NotBlank(message = "사업장 주소는 필수 입력 항목입니다.")
    @Schema(description = "사업장 소재지 주소", example = "서울특별시 강남구 테헤란로 123, 4층")
    private String businessAddress;

    @Schema(description = "마케팅 정보 수신 동의 여부", example = "true")
    private boolean marketingAgree;
}