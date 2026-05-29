package com.petplace.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;

@Schema(description = "커뮤니티 게시글 작성 및 수정 요청 객체")
public record PostRequest(
        @NotBlank(message = "제목을 입력해주세요.")
        @Size(max = 200, message = "제목은 최대 200자까지 입력 가능합니다.")
        @Schema(description = "게시글 제목", example = "우리집 강아지가 오늘 처음으로 두 발로 걸었어요!!")
        String title,

        @NotBlank(message = "내용을 입력해주세요.")
        @Schema(description = "게시글 내용", example = "아침에 간식을 주려고 하니까 갑자기 서서 세 걸음을 걸어오는데 진짜 깜짝 놀랐습니다ㅋㅋㅋ 다들 이런 경험 있으신가요?")
        String content,

        @Schema(description = "수정 모드 전용 - 유지할 기존 이미지 URL 목록 (새 글 작성 시 null)", nullable = true)
        List<String> existingImageUrls
) {}
