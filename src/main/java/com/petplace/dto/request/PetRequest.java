package com.petplace.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.time.LocalDate;

@Data
@Schema(description = "반려동물 등록 및 수정 요청 객체")
public class PetRequest {

    @Schema(description = "반려동물 이름", example = "초코")
    private String name;

    @Schema(description = "견종/묘종", example = "포메라니안")
    private String breed;

    @Schema(description = "반려동물 사진 URL", example = "https://petplace-bucket.s3.amazon.com/pets/choco.jpg")
    private String imageUrl;

    @Schema(description = "반려동물 생년월일 (YYYY-MM-DD)", example = "2022-05-10")
    private LocalDate birth;
}