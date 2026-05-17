package com.petplace.repository;

import com.petplace.entity.RecentView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface RecentViewRepository extends JpaRepository<RecentView, Long> {

    /**
     * 🌟 [메서드 명 규칙 교정]
     * 소멸한 'ViewedAt' 정렬 대신, 부모 클래스(BaseTimeEntity)로부터 상속받은 전역 표준 Auditing 필드인
     * 'CreatedAt' 규칙으로 변환하여 RecentViewService 호출부와의 컴파일 정합성을 맞춥니다.
     */
    List<RecentView> findTop10ByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 🌟 [네이티브 쿼리 교정]
     * DB 테이블 규격 또한 viewed_at 컬럼이 제거되고 BaseTimeEntity 표준 컬럼인 created_at으로 치환되었습니다.
     * MySQL의 ON DUPLICATE KEY UPDATE 문을 수정하여 생성 및 중복 시 수정 시간을 'created_at'에 동기화합니다.
     */
    @Modifying
    @Query(value = "INSERT INTO recent_views (user_id, restaurant_id, created_at) " +
            "VALUES (:userId, :restaurantId, NOW()) " +
            "ON DUPLICATE KEY UPDATE created_at = NOW()", nativeQuery = true)
    void upsert(@Param("userId") Long userId, @Param("restaurantId") Long restaurantId);

    /**
     * 🌟 [네이티브 쿼리 교정]
     * 특정 개수 초과분 삭제 시 정렬 기준으로 사용되던 서브쿼리 내부의 정렬 컬럼 역시
     * 'viewed_at'에서 표준 컬럼인 'created_at'으로 안전하게 변경합니다.
     */
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