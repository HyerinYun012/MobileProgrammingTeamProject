package com.petplace.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "리뷰 작성 및 수정 요청 객체 (텍스트 데이터)")
public record ReviewRequest(

        @Min(value = 1, message = "평점은 최소 1점 이상이어야 합니다.")
        @Max(value = 5, message = "평점은 최대 5점 이하여야 합니다.")
        @Schema(description = "평점 (1~5점 사이)", example = "5")
        int rating,

        @NotBlank(message = "리뷰 내용을 입력해주세요.")
        @Size(min = 5, message = "리뷰 내용은 최소 5자 이상 작성해주세요.")
        @Schema(description = "리뷰 내용", example = "사장님이 너무 친절하시고 강아지 간식도 서비스로 주셨어요! 재방문 의사 200%입니다.")
        String content
) {}