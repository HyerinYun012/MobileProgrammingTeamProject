package com.petplace.repository;

import com.petplace.entity.RecentView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RecentViewRepository extends JpaRepository<RecentView, Long> {

    /**
     * [수정] 서비스의 호출부와 일치시킴 (언더바 제거)
     * findTop10ByUser_Id -> findTop10ByUserId
     */
    List<RecentView> findTop10ByUserIdOrderByViewedAtDesc(Long userId);

    /**
     * MySQL의 ON DUPLICATE KEY UPDATE를 활용한 Upsert
     */
    @Modifying
    @Query(value = "INSERT INTO recent_views (user_id, restaurant_id, viewed_at) " +
            "VALUES (:userId, :restaurantId, NOW()) " +
            "ON DUPLICATE KEY UPDATE viewed_at = NOW()", nativeQuery = true)
    void upsert(@Param("userId") Long userId, @Param("restaurantId") Long restaurantId);

    /**
     * 특정 개수 초과분 삭제 (MySQL 서브쿼리 제약 우회 버전)
     */
    @Modifying
    @Query(value = "DELETE FROM recent_views " +
            "WHERE user_id = :userId " +
            "AND id NOT IN (" +
            "  SELECT id FROM (" +
            "    SELECT id FROM recent_views " +
            "    WHERE user_id = :userId " +
            "    ORDER BY viewed_at DESC " +
            "    LIMIT :limit" +
            "  ) AS temp" +
            ")", nativeQuery = true)
    void deleteOldRecords(@Param("userId") Long userId, @Param("limit") int limit);
}