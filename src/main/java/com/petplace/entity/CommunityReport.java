package com.petplace.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "community_reports")
@Getter
// ❌ [리팩토링] 무분별한 데이터 변경 및 오염을 막기 위해 @Setter를 전면 삭제합니다.
@NoArgsConstructor(access = AccessLevel.PROTECTED) // 🛡️ JPA 스펙 준수 및 외부 빈 객체 생성 차단
@AllArgsConstructor(access = AccessLevel.PRIVATE) // 💡 외부에서 전 필드 생성자 직접 호출 방어
@lombok.experimental.SuperBuilder
public class CommunityReport extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id")
    private Post post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id")
    private Comment comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reporter_id", nullable = false)
    private User reporter;

    @Column(length = 200, nullable = false)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status;

    /**
     * 🛡️ 데이터 무결성 통합 검증 로직 (JPA 라이프사이클 훅)
     * DB에 데이터가 최종 반영(Insert / Update)되기 직전에 강력한 제약 조건을 가동합니다.
     */
    @PrePersist
    @PreUpdate
    public void validateAndPrePersist() {
        // 1. 상태(Status) 기본값 안전장치
        if (this.status == null) {
            this.status = Status.PENDING;
        }

        // 2. 🌟 [지적사항 반영] 게시글과 댓글 신고의 상호 배타성 검증
        // 둘 다 null이거나, 둘 다 null이 아닐 경우 쓰레기 데이터로 판단하여 예외를 던집니다.
        if ((this.post == null && this.comment == null) || (this.post != null && this.comment != null)) {
            throw new IllegalStateException("커뮤니티 신고는 '게시글' 또는 '댓글' 중 정확히 하나만 대상으로 지정해야 합니다.");
        }
    }

    /**
     * 관리자의 반려/단순 처리에 따른 상태 변경 도메인 메서드
     */
    public void completeReport() {
        this.status = Status.COMPLETED;
    }

    public enum Status { PENDING, COMPLETED }
}