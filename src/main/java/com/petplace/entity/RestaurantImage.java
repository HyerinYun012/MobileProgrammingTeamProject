package com.petplace.entity;

import jakarta.persistence.*;
import lombok.*;
import java.util.Objects;

@Entity
@Table(name = "restaurant_images")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 🛡️ 외부에서 텅 빈 상태로 생성되는 위험을 완전히 방지합니다.
public class RestaurantImage extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @Column(nullable = false, length = 500)
    private String imageUrl;

    // 🛡️ [JPA 정정] 하이버네이트 리플렉션 인프라 및 서비스단 순서 매핑을 위해 final을 제거합니다.
    @Column(nullable = false)
    private int sortOrder = 0;

    /**
     * 🌟 [서비스 연동 핵심] 고도화된 연관관계 편의 생성자
     * 외부 Setter를 찌르는 대신, 객체 생성 시점에 이미지 URL, 대상 장소, 정렬 순서까지 원자적으로 바인딩합니다.
     */
    public RestaurantImage(String imageUrl, Restaurant restaurant, int sortOrder) {
        this.imageUrl = imageUrl;
        this.restaurant = restaurant;
        this.sortOrder = sortOrder; // 🌟 생성 시점에 안전하게 주입

        // 구현된 equals() 덕분에 주소가 다른 인스턴스라도 imageUrl 값이 같으면 컬렉션 중복 삽입을 완벽 차단합니다.
        // 또한 이 로직 덕분에 서비스단에서 수동으로 restaurant.getImages().add(this)를 중복 호출할 필요가 없습니다.
        if (restaurant != null && !restaurant.getImages().contains(this)) {
            restaurant.getImages().add(this);
        }
    }

    /**
     * 비즈니스 키(Business Key) 기반의 equals & hashCode 재정의
     */
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        RestaurantImage that = (RestaurantImage) o;

        if (this.id != null && that.id != null) {
            return Objects.equals(this.id, that.id);
        }
        return Objects.equals(imageUrl, that.imageUrl);
    }

    @Override
    public int hashCode() {
        return id != null ? Objects.hash(id) : Objects.hash(imageUrl);
    }
}