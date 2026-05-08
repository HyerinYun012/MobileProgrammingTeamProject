package com.petplace.repository;
import com.petplace.entity.SearchLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;
public interface SearchLogRepository extends JpaRepository<SearchLog, Long> {
    @Query(value = "SELECT keyword FROM search_logs GROUP BY keyword ORDER BY COUNT(*) DESC LIMIT 5", nativeQuery = true)
    List<String> findTop5Keywords();
}
