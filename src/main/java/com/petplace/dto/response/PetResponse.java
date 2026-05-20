package com.petplace.dto.response;

import com.petplace.entity.Pet;
import lombok.Builder;
import lombok.Getter;
import java.time.LocalDate;

@Getter
@Builder
public class PetResponse {
    private Long id;
    private String name;
    private LocalDate birth;
    private String breed;
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