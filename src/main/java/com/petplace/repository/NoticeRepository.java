package com.petplace.repository;

import com.petplace.entity.Notice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long> {
    // 특정 식당의 공지사항을 최신 등록순으로 정렬하여 조회합니다.
    List<Notice> findByRestaurant_IdOrderByCreatedAtDesc(Long restaurantId);
}