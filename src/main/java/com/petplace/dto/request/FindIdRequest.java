package com.petplace.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
@Schema(description = "아이디 찾기 요청 객체")
public class FindIdRequest {

    @NotBlank(message = "성함은 필수 입력 항목입니다.")
    @Schema(description = "사용자 이름", example = "홍길동")
    private String name;

    @NotBlank(message = "휴대폰 번호는 필수 입력 항목입니다.")
    @Pattern(regexp = "^01(?:0|1|[6-9])(?:\\d{3}|\\d{4})\\d{4}$", message = "올바른 휴대폰 번호 형식이 아닙니다.")
    @Schema(description = "휴대폰 번호", example = "01012345678")
    private String phone;
}