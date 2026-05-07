package com.petplace.repository;
import com.petplace.entity.RecentView;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
public interface RecentViewRepository extends JpaRepository<RecentView, Long> {
    List<RecentView> findTop10ByUser_IdOrderByViewedAtDesc(Long userId);
    @Modifying
    @Query(value = "INSERT INTO recent_views (user_id, restaurant_id, viewed_at) VALUES (:userId, :restaurantId, NOW()) ON DUPLICATE KEY UPDATE viewed_at = NOW()", nativeQuery = true)
    void upsert(@Param("userId") Long userId, @Param("restaurantId") Long restaurantId);
}
