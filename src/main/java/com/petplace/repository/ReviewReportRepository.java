package com.petplace.repository;

import com.petplace.entity.ReviewReport;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ReviewReportRepository extends JpaRepository<ReviewReport, Long> {

    /**
     * 중복 신고 여부 확인 (단일 결과 반환이므로 페이징 불필요)
     */
    boolean existsByReviewIdAndOwnerId(Long reviewId, Long ownerId);

    void deleteByReviewId(Long reviewId);

    @Modifying
    @Query("DELETE FROM ReviewReport rr WHERE rr.review.restaurant.id = :restaurantId")
    void deleteAllByRestaurantId(@Param("restaurantId") Long restaurantId);

    /**
     * 💡 [페이징 적용] 관리자가 리뷰 삭제 시 관련 신고들을 조회
     * 특정 리뷰에 신고가 다수 발생할 수 있으므로 페이징 처리를 권장합니다.
     */
    Page<ReviewReport> findAllByReviewId(Long reviewId, Pageable pageable);

    /**
     * 💡 [페이징 적용] 관리자 대시보드용 전체 리뷰 신고 내역 조회
     * 1. countQuery 명시: Fetch Join 쿼리에서 페이징 시 전체 개수를 정확히 세기 위해 필수입니다.
     * 2. 정렬: 이제 메서드명으로 고정하지 않고 서비스에서 Pageable을 통해 동적으로 정렬합니다.
     */
    @Query(value = "select rr from ReviewReport rr " +
            "left join fetch rr.review " +
            "left join fetch rr.owner",
            countQuery = "select count(rr) from ReviewReport rr")
    Page<ReviewReport> findAllBy(Pageable pageable);
}