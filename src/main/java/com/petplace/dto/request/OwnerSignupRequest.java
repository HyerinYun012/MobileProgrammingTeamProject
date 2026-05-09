package com.petplace.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "사장님(업체) 회원가입 요청 객체")
public class OwnerSignupRequest {

    @Schema(description = "로그인 아이디", example = "owner_petplace")
    private String loginId;

    @Schema(description = "비밀번호", example = "ownerpass123!")
    private String password;

    @Schema(description = "비밀번호 확인", example = "ownerpass123!")
    private String passwordConfirm;

    @Schema(description = "사장님 닉네임 (서비스 노출용)", example = "강아지천국점주")
    private String nickname;

    @Schema(description = "휴대폰 번호", example = "01098765432")
    private String phone;

    @Schema(description = "사업자 등록 번호", example = "123-45-67890")
    private String businessNo;

    @Schema(description = "사업장 소재지 주소", example = "서울특별시 강남구 테헤란로 123, 4층")
    private String businessAddress;

    @Schema(description = "마케팅 정보 수신 동의 여부", example = "true")
    private boolean marketingAgree;
}