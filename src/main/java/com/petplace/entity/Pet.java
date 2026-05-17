package com.petplace.entity;

import com.petplace.dto.request.PetRequest;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;

@Entity
@Table(name = "pets")
@Getter
// ✂️ 클래스 레벨의 @Setter를 과감히 제거하여 외부에서 객체 상태를 무분별하게 헤집는 것을 차단합니다.
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 🛡️ 무분별한 빈 객체 생성(new Pet())을 막고 JPA 스펙을 준수합니다.
public class Pet extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    // 💡 Setter가 없으므로 식별자(PK)는 데이터베이스와 JPA 메커니즘 외에 그 누구도 임의로 변경할 수 없습니다.
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

    /**
     * 💡 [추가] 생성 전용 정적 팩토리 메서드 (or 편의 생성자)
     * 클래스 레벨 Setter가 닫혔으므로, 최초 객체 생성 시점에 제약조건을 강제하는 명확한 창구를 개설합니다.
     */
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
     * 🛡️ 반려동물 정보 업데이트 로직 (유일한 상태 변경 변경 창구)
     * 외부 Setter 조작 없이, 오직 이 비즈니스 메서드를 통해서만 객체의 상태가 안전하게 갱신됩니다.
     */
    public void updateInfo(PetRequest req, String currentImageUrl) {
        if (req.getName() != null) this.name = req.getName();
        if (req.getBirth() != null) this.birth = req.getBirth();
        if (req.getBreed() != null) this.breed = req.getBreed();

        // 서비스에서 정돈되어 넘어온 S3 주소(혹은 유지된 주소)를 최종 할당
        this.imageUrl = currentImageUrl;
    }
}