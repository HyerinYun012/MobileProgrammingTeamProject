package com.petplace.repository;

import com.petplace.entity.SearchLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import java.time.LocalDateTime;
import java.util.List;

public interface SearchLogRepository extends JpaRepository<SearchLog, Long> {

    /**
     * 💡 [안전] 인기 검색어 TOP 5
     * 이 쿼리는 데이터베이스 엔진에서 GROUP BY와 LIMIT 5를 통해 연산된 '결과값(5개)'만 반환하므로,
     * 엔티티 전체를 메모리에 올리지 않아 OOM 위험이 없습니다. 그대로 List<String>을 사용하셔도 안전합니다.
     */
    @Query(value = "SELECT s.keyword FROM search_logs s " +
            "WHERE s.created_at >= :since " +
            "GROUP BY s.keyword " +
            "ORDER BY COUNT(s.id) DESC " +
            "LIMIT 5", nativeQuery = true)
    List<String> findTop5Keywords(@Param("since") LocalDateTime since);

    /**
     * 💡 [페이징 적용] 특정 시간 이후의 검색 로그 조회
     * 페이징 인프라가 적용되어 있어, 데이터가 아무리 많이 쌓여도
     * 지정된 Pageable(size)만큼만 메모리에 로드하므로 OOM으로부터 안전합니다.
     */
    Page<SearchLog> findAllByCreatedAtAfter(LocalDateTime since, Pageable pageable);
}