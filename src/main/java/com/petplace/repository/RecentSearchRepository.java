package com.petplace.repository;

import com.petplace.entity.RecentSearch;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RecentSearchRepository extends JpaRepository<RecentSearch, Long> {

    /**
     * 💡 [페이징 적용]
     * 사용자의 최근 검색 기록을 페이징하여 조회합니다.
     * 정렬은 서비스 계층에서 Pageable 파라미터(예: Sort.by("createdAt").descending())를 통해 전달받습니다.
     */
    Page<RecentSearch> findByUser_Id(Long userId, Pageable pageable);

    // 삭제 및 수정 연산은 메모리 부담이 없으므로 그대로 유지합니다.
    void deleteByUser_IdAndKeyword(Long userId, String keyword);

    @Modifying
    @Query(value = "INSERT INTO recent_searches (user_id, keyword, created_at, updated_at) " +
            "VALUES (:userId, :keyword, NOW(), NOW()) " +
            "ON DUPLICATE KEY UPDATE updated_at = NOW()", nativeQuery = true)
    void upsert(@Param("userId") Long userId, @Param("keyword") String keyword);
}