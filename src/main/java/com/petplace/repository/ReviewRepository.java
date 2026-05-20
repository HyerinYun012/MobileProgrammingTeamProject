package com.petplace.repository;

import com.petplace.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    /**
     * 💡 [페이징 적용] 마이페이지용: 유저의 리뷰 목록 조회
     * 정렬은 서비스 계층의 Pageable 객체에서 Sort 조건을 통해 제어합니다.
     */
    Page<Review> findAllByUserId(Long userId, Pageable pageable);

    /**
     * 💡 [페이징 적용] 식당 상세페이지용: 특정 식당의 리뷰 목록 조회
     * @param restaurantId 식당 ID
     * @param pageable 페이징 정보 (페이지 번호, 사이즈, 정렬)
     * @return Page<Review>
     */
    Page<Review> findByRestaurantId(Long restaurantId, Pageable pageable);
}