package com.petplace.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "inquiries")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
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

    @Column(length = 100, nullable = false)
    private String title;

    @Lob
    @Column(nullable = false)
    private String content;

    @Lob
    private String reply;

    @Column(length = 500)
    private String imageUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status;

    public static Inquiry createInquiry(User user, Restaurant restaurant, Category category, String title, String content, String imageUrl) {
        Inquiry inquiry = new Inquiry();
        inquiry.user = user;
        inquiry.restaurant = restaurant;
        inquiry.category = category;
        inquiry.title = title;
        inquiry.content = content;
        inquiry.imageUrl = imageUrl;
        inquiry.status = Status.PENDING;
        return inquiry;
    }

    public void completeInquiry(String reply) {
        this.reply = reply;
        this.status = Status.COMPLETED;
    }

    public enum Category { GENERAL, BUSINESS, ERROR }
    public enum Status { PENDING, COMPLETED }
}