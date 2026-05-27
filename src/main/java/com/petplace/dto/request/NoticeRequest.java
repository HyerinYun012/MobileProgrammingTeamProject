package com.petplace.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@Schema(description = "가게 공지사항 작성 및 수정 요청 객체 (텍스트 데이터)")
public record NoticeRequest(
        @NotBlank(message = "공지사항 제목은 필수 입력 항목입니다.")
        @Size(max = 150, message = "제목은 최대 150자까지 입력 가능합니다.")
        @Schema(description = "공지사항 제목", example = "이번 주말 내부 인테리어 공사로 인한 임시 휴업 안내")
        String title,

        @NotBlank(message = "공지사항 본문 내용은 필수 입력 항목입니다.")
        @Schema(description = "공지사항 본문 내용", example = "안녕하세요, 펫플레이스입니다. 조금 더 쾌적한 환경을 제공해 드리고자 이번 주 토요일 내부 인테리어 보수 공사를 진행하게 되었습니다. 이용에 불편을 드려 죄송합니다.")
        String content
) {}