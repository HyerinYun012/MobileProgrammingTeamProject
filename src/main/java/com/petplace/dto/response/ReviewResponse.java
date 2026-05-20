package com.petplace.dto.response;

import com.petplace.entity.Review;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ReviewResponse {
    private Long id;
    private int rating;
    private String content;
    private String imageUrl;
    private String writerName;      // 리뷰 작성자 이름
    private LocalDateTime createdAt; // BaseTimeEntity 상속 필드

    /**
     * Entity를 DTO로 변환하는 정적 팩토리 메서드
     */
    public static ReviewResponse from(Review review) {
        return ReviewResponse.builder()
                .id(review.getId())
                .rating(review.getRating())
                .content(review.getContent())
                .imageUrl(review.getImageUrl())
                .writerName(review.getUser() != null ? review.getUser().getNickname() : "알 수 없음")
                .createdAt(review.getCreatedAt())
                .build();
    }
}