package com.petplace.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile; // 💡 추가
import java.time.LocalDate;

@Data
@Schema(description = "반려동물 등록 및 수정 요청 객체")
public class PetRequest {

    @NotBlank(message = "반려동물의 이름을 입력해주세요.")
    @Size(max = 20, message = "이름은 20자 이내로 입력해주세요.")
    @Schema(description = "반려동물 이름", example = "초코")
    private String name;

    @NotBlank(message = "견종 또는 묘종을 입력해주세요.")
    @Schema(description = "견종/묘종", example = "포메라니안")
    private String breed;

    @NotNull(message = "생년월일을 선택해주세요.")
    @PastOrPresent(message = "생년월일은 미래 날짜일 수 없습니다.")
    @Schema(description = "반려동물 생년월일 (YYYY-MM-DD)", example = "2022-05-10")
    private LocalDate birth;

    @Schema(description = "반려동물 업로드용 프로필 이미지 파일 (선택 사항)")
    private MultipartFile imageFile;
}