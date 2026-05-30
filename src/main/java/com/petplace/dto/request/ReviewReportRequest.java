package com.petplace.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@Schema(description = "리뷰 신고 요청 DTO")
public class ReviewReportRequest {

    @NotBlank(message = "신고 사유는 필수 입력 항목입니다.")
    @Schema(description = "리뷰 신고 사유 (공백 불가)", example = "부적절한 욕설 및 비방이 포함되어 있습니다.")
    private String reason;
}