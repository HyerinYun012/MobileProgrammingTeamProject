package com.petplace.dto.response;

import com.petplace.entity.Review;
import io.swagger.v3.oas.annotations.media.Schema; // 💡 Swagger 어노테이션 임포트
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "리뷰 정보 응답 객체") // 💡 클래스 단위 설명
public class ReviewResponse {

    @Schema(description = "리뷰 고유 ID", example = "102")
    private Long id;

    @Schema(description = "평점 (1~5점)", example = "5")
    private int rating;

    @Schema(description = "리뷰 상세 내용", example = "사장님이 너무 친절하시고 강아지 간식도 서비스로 주셨어요! 재방문 의사 200%입니다.")
    private String content;

    @Schema(description = "리뷰 첨부 이미지 URL (없는 경우 null)", example = "https://petplace-bucket.s3.amazonaws.com/reviews/uuid_image.jpg")
    private String imageUrl;

    @Schema(description = "리뷰 작성자 닉네임", example = "초코엄마")
    private String writerName;

    @Schema(description = "리뷰 작성자 프로필 이미지 URL (없는 경우 null)")
    private String writerProfileUrl;

    @Schema(description = "리뷰 작성 일시", example = "2026-05-27T10:20:00")
    private LocalDateTime createdAt;

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
                .writerProfileUrl(review.getUser() != null ? review.getUser().getProfileUrl() : null)
                .createdAt(review.getCreatedAt())
                .build();
    }
}