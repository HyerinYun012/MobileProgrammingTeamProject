package com.petplace.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "menus")
@Getter
// ❌ [리팩토링] 무분별한 데이터 오염을 유발하는 @Setter를 전면 제거합니다.
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

    /**
     * 🌟 [도메인 비즈니스 메서드] 메뉴 정보 일괄 수정
     * Setter를 열어두면 외부 레이어에서 어떤 필드를 왜 바꾸는지 추적하기 어렵고 데이터가 파편화됩니다.
     * 이렇게 하나의 메서드로 묶어두면 의미 있는 비즈니스 목적(메뉴 정보 수정)이 명확해지고 안전해집니다.
     */
    public void updateMenuInfo(String name, int price, String description, String imageUrl) {
        this.name = name;
        this.price = price;
        this.description = description;
        if (imageUrl != null) {
            this.imageUrl = imageUrl;
        }
    }
}