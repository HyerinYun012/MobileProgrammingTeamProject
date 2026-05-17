package com.petplace.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile; // 💡 추가

@Data
@Schema(description = "회원 프로필 수정 요청 객체")
public class UpdateProfileRequest {

    @NotBlank(message = "닉네임은 필수 입력 항목입니다.")
    @Size(min = 2, max = 10, message = "닉네임은 2자에서 10자 사이여야 합니다.")
    @Schema(description = "변경할 닉네임", example = "초코언니")
    private String nickname;

    @NotBlank(message = "이메일은 필수 입력 항목입니다.")
    @Email(message = "올바른 이메일 형식이 아닙니다.")
    @Schema(description = "변경할 이메일 주소", example = "new_email@example.com")
    private String email;

    @NotBlank(message = "휴대폰 번호는 필수 입력 항목입니다.")
    @Pattern(regexp = "^01(?:0|1|[6-9])(?:\\d{3}|\\d{4})\\d{4}$", message = "올바른 휴대폰 번호 형식이 아닙니다.")
    @Schema(description = "변경할 휴대폰 번호", example = "01099998888")
    private String phone;

    @Schema(description = "새로 교체할 실제 물리 프로필 이미지 파일 (선택 사항)")
    private MultipartFile profileImage;
}