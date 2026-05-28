package com.petplace.repository;

import com.petplace.entity.RecentView;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecentViewRepository extends JpaRepository<RecentView, Long> {

    /**
     * 💡 [페이징 적용]
     * 기존 Top10을 고정하던 로직을 Pageable 파라미터로 대체했습니다.
     * 서비스 계층에서 PageRequest.of(0, 10, Sort.by("createdAt").descending())를 전달하면 동일하게 동작합니다.
     */
    Page<RecentView> findByUserId(Long userId, Pageable pageable);

    @Modifying
    @Query(value = "INSERT INTO recent_views (user_id, restaurant_id, created_at, updated_at) " +
            "VALUES (:userId, :restaurantId, NOW(), NOW()) " +
            "ON DUPLICATE KEY UPDATE created_at = NOW(), updated_at = NOW()", nativeQuery = true)
    void upsert(@Param("userId") Long userId, @Param("restaurantId") Long restaurantId);

    @Modifying
    @Query(value = "DELETE FROM recent_views " +
            "WHERE user_id = :userId " +
            "AND id NOT IN (" +
            "  SELECT id FROM (" +
            "    SELECT id FROM recent_views " +
            "    WHERE user_id = :userId " +
            "    ORDER BY created_at DESC " +
            "    LIMIT :limit" +
            "  ) AS temp" +
            ")", nativeQuery = true)
    void deleteOldRecords(@Param("userId") Long userId, @Param("limit") int limit);
}
