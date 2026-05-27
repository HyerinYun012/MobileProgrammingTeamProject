package com.petplace.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inquiries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 🛡️ 안전장치 확보
public class Inquiry extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = true)
    private Restaurant restaurant;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Category category;

    @Column(length = 100)
    private String email;

    @Lob
    @Column(nullable = false)
    private String content;

    @Column(length = 500)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;


    public static Inquiry createInquiry(User user, Restaurant restaurant, Category category, String content, String email, String imageUrl) {
        Inquiry inquiry = new Inquiry();
        inquiry.user = user;
        inquiry.restaurant = restaurant;
        inquiry.category = category;
        inquiry.content = content;
        inquiry.email = email;
        inquiry.imageUrl = imageUrl;
        inquiry.status = Status.PENDING;
        return inquiry;
    }

    public void completeInquiry() {
        this.status = Status.COMPLETED;
    }

    public enum Category { GENERAL, BUSINESS, ERROR }
    public enum Status { PENDING, COMPLETED }
}