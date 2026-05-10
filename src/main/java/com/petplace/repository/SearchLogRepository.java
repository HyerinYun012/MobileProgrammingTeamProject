package com.petplace.repository;

import com.petplace.entity.SearchLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface SearchLogRepository extends JpaRepository<SearchLog, Long> {

    @Query(value = "SELECT s.keyword FROM search_logs s " +
            "WHERE s.created_at >= :since " +
            "GROUP BY s.keyword " +
            "ORDER BY COUNT(s.id) DESC " +
            "LIMIT 5", nativeQuery = true)
    List<String> findTop5Keywords(@Param("since") LocalDateTime since);
}