package com.petplace.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservations")
@Getter
@Setter
@NoArgsConstructor
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user; // 예약한 고객 정보

    @Column(nullable = false)
    private LocalDateTime reservationTime; // 예약 일시

    @Column(nullable = false)
    private int peopleCount; // 예약 인원수

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Status status = Status.PENDING; // 예약 상태 기본값: 대기

    @CreationTimestamp
    private LocalDateTime createdAt;

    public enum Status {
        PENDING,   // 예약 대기 (사장님 확인 전)
        CONFIRMED, // 예약 확정
        CANCELED   // 예약 취소
    }
}