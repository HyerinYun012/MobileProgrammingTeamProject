package com.petplace.dto.response;

import com.petplace.entity.Notice;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NoticeResponse {
    private Long id;
    private String title;
    private String content;
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