package com.petplace.repository;

import com.petplace.entity.ReviewReport;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReviewReportRepository extends JpaRepository<ReviewReport, Long> {
    // 중복 신고 여부 확인용
    boolean existsByReviewIdAndOwnerId(Long reviewId, Long ownerId);

    // 관리자가 리뷰 삭제 시 관련 신고들을 찾기 위함
    List<ReviewReport> findAllByReviewId(Long reviewId);
}