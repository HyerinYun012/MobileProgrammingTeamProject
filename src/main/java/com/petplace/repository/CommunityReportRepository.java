package com.petplace.repository;

import com.petplace.entity.CommunityReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface CommunityReportRepository extends JpaRepository<CommunityReport, Long> {

    /**
     * 💡 [페이징 적용] 게시글별 신고 내역 조회
     */
    Page<CommunityReport> findAllByPostId(Long postId, Pageable pageable);

    /**
     * 💡 [페이징 적용] 댓글별 신고 내역 조회
     */
    Page<CommunityReport> findAllByCommentId(Long commentId, Pageable pageable);

    /**
     * 💡 [성능 최적화 및 페이징 적용]
     * 1. countQuery 명시: 복잡한 조인 쿼리 시, 개수만 빠르게 세기 위해 countQuery를 분리하여 성능을 최적화합니다.
     * 2. Pageable 파라미터 추가: 서비스 계층에서 요청 시 정렬(Sort) 정보를 포함한 Pageable을 넘겨주세요.
     */
    @Query(value = "select cr from CommunityReport cr " +
            "left join fetch cr.post " +
            "left join fetch cr.comment " +
            "left join fetch cr.reporter " +
            "where cr.status = :status",
            countQuery = "select count(cr) from CommunityReport cr where cr.status = :status")
    Page<CommunityReport> findAllByStatusOrderByCreatedAtDesc(
            @Param("status") CommunityReport.Status status,
            Pageable pageable
    );
}