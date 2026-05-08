package com.petplace.repository;
import com.petplace.entity.ReviewReport;
import org.springframework.data.jpa.repository.JpaRepository;
public interface ReviewReportRepository extends JpaRepository<ReviewReport, Long> {
    boolean existsByReview_IdAndOwner_Id(Long reviewId, Long ownerId);
}
