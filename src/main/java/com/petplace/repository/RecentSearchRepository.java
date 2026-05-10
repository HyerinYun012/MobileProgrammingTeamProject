package com.petplace.repository;
import com.petplace.entity.RecentSearch;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.util.List;
public interface RecentSearchRepository extends JpaRepository<RecentSearch, Long> {
    List<RecentSearch> findByUser_IdOrderBySearchedAtDesc(Long userId);
    void deleteByUser_IdAndKeyword(Long userId, String keyword);
    @Modifying
    @Query(value = "INSERT INTO recent_searches (user_id, keyword, searched_at) VALUES (:userId, :keyword, NOW()) ON DUPLICATE KEY UPDATE searched_at = NOW()", nativeQuery = true)
    void upsert(@Param("userId") Long userId, @Param("keyword") String keyword);
}
