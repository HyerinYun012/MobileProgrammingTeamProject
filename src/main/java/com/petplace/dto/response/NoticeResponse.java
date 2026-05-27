package com.petplace.dto.response;

import com.petplace.entity.Notice;
import io.swagger.v3.oas.annotations.media.Schema; // 💡 Swagger 어노테이션 임포트
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "공지사항 정보 응답 객체") // 💡 클래스 단위 설명
public class NoticeResponse {

    @Schema(description = "공지사항 고유 ID", example = "1")
    private Long id;

    @Schema(description = "공지사항 제목", example = "[안내] 서비스 런칭 기념 및 앱 업데이트 안내")
    private String title;

    @Schema(description = "공지사항 상세 내용", example = "안녕하세요. 펫플레이스입니다. 새로워진 커뮤니티 기능과 장소 예약 시스템이 업데이트되었습니다...")
    private String content;

    @Schema(description = "공지사항 등록 일시", example = "2026-05-27T10:00:00")
    private LocalDateTime createdAt;

    /**
     * Entity를 DTO로 변환하는 정적 팩토리 메서드
     */
    public static NoticeResponse from(Notice notice) {
        return NoticeResponse.builder()
                .id(notice.getId())
                .title(notice.getTitle())
                .content(notice.getContent())
                .createdAt(notice.getCreatedAt())
                .build();
    }
}