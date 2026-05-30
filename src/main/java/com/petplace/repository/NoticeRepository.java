package com.petplace.repository;

import com.petplace.entity.Notice;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface NoticeRepository extends JpaRepository<Notice, Long> {

    /**
     * 💡 [페이징 적용]
     * 특정 식당의 공지사항을 최신순으로 페이징 조회합니다.
     * * @param restaurantId 조회할 식당 ID
     * @param pageable 페이징 정보 (페이지 번호, 사이즈, 정렬)
     * @return Page<Notice> 페이징된 공지사항 목록
     */
    Page<Notice> findByRestaurantIdOrderByCreatedAtDesc(Long restaurantId, Pageable pageable);

    void deleteAllByRestaurantId(Long restaurantId);
}