package com.petplace.entity;

import com.petplace.exception.BusinessException;
import com.petplace.exception.ErrorCode;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "menus")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 🛡️ JPA 스펙 준수 및 무분별한 빈 객체 생성 제한
@AllArgsConstructor(access = AccessLevel.PRIVATE) // 💡 외부에서 전 필드 생성자를 직접 호출하는 행위 차단
@lombok.experimental.SuperBuilder
public class Menu extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @Column(nullable = false, length = 100)
    private String name; // 음식 메뉴 이름

    @Column(nullable = false)
    private int price; // 음식 가격

    @Column(length = 500)
    private String description; // 음식 설명

    @Column(length = 500)
    private String imageUrl; // 메뉴 사진 URL

    public void updateMenuInfo(String name, int price, String description, String imageUrl) {
        if (price < 0) {
            throw new BusinessException(ErrorCode.INVALID_PRICE_VALUE);
        }

        this.name = name;
        this.price = price;
        this.description = description;
        if (imageUrl != null) {
            this.imageUrl = imageUrl;
        }
    }
}