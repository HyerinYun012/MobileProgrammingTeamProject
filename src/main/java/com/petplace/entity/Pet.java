package com.petplace.entity;

import com.petplace.dto.request.PetRequest;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "pets")
@Getter
@Setter
@NoArgsConstructor
public class Pet {
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

    @CreationTimestamp
    private LocalDateTime createdAt;

    /**
     * [추가] 반려동물 정보 업데이트 로직
     * 서비스 레이어의 updatePet 메서드에서 호출되어 Dirty Checking을 유도합니다.
     */
    public void updateInfo(PetRequest req) {
        if (req.getName() != null) this.name = req.getName();
        if (req.getBirth() != null) this.birth = req.getBirth();
        if (req.getBreed() != null) this.breed = req.getBreed();
        if (req.getImageUrl() != null) this.imageUrl = req.getImageUrl();
    }
}