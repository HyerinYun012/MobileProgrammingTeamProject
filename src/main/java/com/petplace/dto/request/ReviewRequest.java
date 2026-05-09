package com.petplace.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
@Schema(description = "리뷰 작성 및 수정 요청 객체")
public class ReviewRequest {

    @Schema(description = "평점 (1~5점 사이)", example = "5")
    private int rating;

    @Schema(description = "리뷰 내용 (최소 10자 이상 권장)", example = "사장님이 너무 친절하시고 강아지 간식도 서비스로 주셨어요! 재방문 의사 200%입니다.")
    private String content;

    @Schema(description = "리뷰 첨부 이미지 URL (선택 사항)", example = "https://petplace-bucket.s3.amazon.com/reviews/visit_photo.jpg")
    private String imageUrl;
}