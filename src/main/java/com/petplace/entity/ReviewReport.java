package com.petplace.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "review_reports",
        uniqueConstraints = @UniqueConstraint(columnNames = {"review_id", "owner_id"})
)
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // JPA 스펙 준수 및 무분별한 외부 빈 객체 생성 제한
@AllArgsConstructor // 💡 Lombok @SuperBuilder 사용을 위한 필수 구조 보존
// 🌟 부모 클래스(BaseTimeEntity)의 시간 필드를 빌더에서 정상 상속받기 위해 @SuperBuilder로 리팩토링합니다.
@lombok.experimental.SuperBuilder
public class ReviewReport extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "review_id", nullable = false)
    private Review review;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(length = 200)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Status status = Status.PENDING;

    public void completeReport() {
        this.status = Status.COMPLETED;
    }

    public enum Status { PENDING, COMPLETED }
}