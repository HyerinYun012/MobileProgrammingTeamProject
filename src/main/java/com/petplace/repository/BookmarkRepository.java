package com.petplace.repository;

import com.petplace.entity.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    // 1. 마이페이지용: 유저의 전체 북마크 목록 조회
    List<Bookmark> findAllByUserIdOrderByCreatedAtDesc(Long userId);

    /*
     * 2. [추가] 북마크 토글용: 특정 유저가 특정 식당을 북마크했는지 확인
     */
    Optional<Bookmark> findByUserIdAndRestaurantId(Long userId, Long restaurantId);
}