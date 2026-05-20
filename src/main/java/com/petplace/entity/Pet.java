package com.petplace.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "pets")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Pet extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 50)
    private String name;

    private LocalDate birth;

    @Column(length = 50)
    private String breed;

    @Column(length = 500)
    private String imageUrl;

    public static Pet createPet(User user, String name, LocalDate birth, String breed, String imageUrl) {
        Pet pet = new Pet();
        pet.user = user;
        pet.name = name;
        pet.birth = birth;
        pet.breed = breed;
        pet.imageUrl = imageUrl;
        return pet;
    }

    /**
     * 🛡️ [리팩토링] Web DTO 의존성을 완전히 걷어내고 순수 자바 타입만 수용
     * 프론트엔드 API 요구사항(DTO)이 변경되어도 이 핵심 엔티티는 아무런 영향을 받지 않습니다.
     */
    public void updateInfo(String name, LocalDate birth, String breed, String currentImageUrl) {
        if (name != null) this.name = name;
        if (birth != null) this.birth = birth;
        if (breed != null) this.breed = breed;

        this.imageUrl = currentImageUrl;
    }
}