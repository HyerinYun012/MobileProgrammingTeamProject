package com.petplace.dto.response;

import com.petplace.entity.Pet;
import io.swagger.v3.oas.annotations.media.Schema; // 💡 Swagger 어노테이션 임포트
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDate;

@Getter
@Builder
@Schema(description = "반려동물 정보 응답 객체") // 💡 클래스 단위 설명
public class PetResponse {

    @Schema(description = "반려동물 고유 ID", example = "1")
    private Long id;

    @Schema(description = "반려동물 이름", example = "초코")
    private String name;

    @Schema(description = "반려동물 생년월일", example = "2022-04-15")
    private LocalDate birth;

    @Schema(description = "반려동물 품종", example = "토이푸들")
    private String breed;

    @Schema(description = "반려동물 프로필 이미지 URL (등록 안 된 경우 null)", example = "https://petplace-bucket.s3.amazonaws.com/pets/choco_profile.jpg")
    private String imageUrl;

    /**
     * 엔티티를 DTO로 변환하는 정적 팩토리 메서드
     */
    public static PetResponse from(Pet pet) {
        return PetResponse.builder()
                .id(pet.getId())
                .name(pet.getName())
                .birth(pet.getBirth())
                .breed(pet.getBreed())
                .imageUrl(pet.getImageUrl())
                .build();
    }
}