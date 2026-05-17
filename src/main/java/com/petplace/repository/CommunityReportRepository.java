package com.petplace.repository;

import com.petplace.entity.CommunityReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommunityReportRepository extends JpaRepository<CommunityReport, Long> {

    List<CommunityReport> findAllByPostId(Long postId);

    List<CommunityReport> findAllByCommentId(Long commentId);

    /**
     * 💡 [성능 최적화 및 경고 해결] 특정 상태(PENDING 등)의 커뮤니티 신고 내역 최신순 조회
     * Fetch Join을 활용하여 연관된 Post, Comment, Reporter(User) 엔티티를 한 방에 가져옵니다.
     */
    @Query("select cr from CommunityReport cr " +
            "left join fetch cr.post " +
            "left join fetch cr.comment " +
            "left join fetch cr.reporter " +
            "where cr.status = :status " +
            "order by cr.createdAt desc")
    List<CommunityReport> findAllByStatusOrderByCreatedAtDesc(@Param("status") CommunityReport.Status status);
}