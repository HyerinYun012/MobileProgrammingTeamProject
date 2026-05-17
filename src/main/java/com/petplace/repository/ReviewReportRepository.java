package com.petplace.repository;

import com.petplace.entity.ReviewReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface ReviewReportRepository extends JpaRepository<ReviewReport, Long> {

    /**
     * 중복 신고 여부 확인용 (동일 사장님이 동일 리뷰를 여러 번 신고하는 것 방지)
     */
    boolean existsByReviewIdAndOwnerId(Long reviewId, Long ownerId);

    /**
     * 관리자가 리뷰 삭제 시 관련 신고들을 찾기 위함
     */
    List<ReviewReport> findAllByReviewId(Long reviewId);

    /**
     * 💡 [추가 및 성능 최적화] 관리자 대시보드용 전체 리뷰 신고 내역 최신순 조회
     * Fetch Join을 활용하여 연관된 Review와 Owner(User) 엔티티를 한 번에 가져옴으로써 N+1 문제를 원천 차단합니다.
     */
    @Query("select rr from ReviewReport rr " +
            "left join fetch rr.review " +
            "left join fetch rr.owner " +
            "order by rr.createdAt desc")
    List<ReviewReport> findAllByOrderByCreatedAtDesc();
}