package com.petplace.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "회원 프로필 수정 요청 객체")
public class UpdateProfileRequest {

    @Schema(description = "변경할 닉네임", example = "초코언니")
    private String nickname;

    @Schema(description = "변경할 이메일 주소", example = "new_email@example.com")
    private String email;

    @Schema(description = "변경할 휴대폰 번호", example = "01099998888")
    private String phone;

    @Schema(description = "변경할 프로필 이미지 URL", example = "https://petplace-bucket.s3.amazon.com/profiles/user123.jpg")
    private String profileUrl;
}