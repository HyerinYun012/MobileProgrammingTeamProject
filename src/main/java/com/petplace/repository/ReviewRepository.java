package com.petplace.repository;

import com.petplace.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ReviewRepository extends JpaRepository<Review, Long> {

    /**
     * 1. 마이페이지용: 내가 쓴 리뷰 목록 조회
     */
    List<Review> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 2. 식당 상세페이지용: 특정 식당의 리뷰 목록 조회 (에러 해결 지점)
     * [추가] 서비스 레이어의 호출부인 'findByRestaurant_IdOrderByCreatedAtDesc'를 선언합니다.
     */
    List<Review> findByRestaurant_IdOrderByCreatedAtDesc(Long restaurantId);
}