package com.petplace.service;

import com.petplace.entity.Restaurant;
import com.petplace.entity.SearchLog;
import com.petplace.entity.RecentSearch; // Added Import
import com.petplace.repository.RestaurantRepository;
import com.petplace.repository.SearchLogRepository;
import com.petplace.repository.RecentSearchRepository; // Added Import
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class SearchService {

    private final SearchLogRepository searchLogRepository;
    private final RestaurantRepository restaurantRepository;
    private final RecentSearchRepository recentSearchRepository; // 1. Added Repository Injection

    /**
     * 키워드 통합 검색 (로그 저장 및 최근 검색어 반영)
     */
    @Transactional
    public List<Restaurant> search(String keyword, Long userId) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return List.of();
        }

        String trimmedKeyword = keyword.trim();

        // 1. 통계용 로그 저장
        searchLogRepository.save(new SearchLog(trimmedKeyword));

        // 2. 로그인 유저인 경우 최근 검색어 저장
        if (userId != null) {
            saveRecentSearch(userId, trimmedKeyword);
        }

        return restaurantRepository.findByNameContainingIgnoreCase(trimmedKeyword);
    }

    /**
     * 인기 검색어 TOP 5 조회
     */
    public List<String> getPopularKeywords() {
        return searchLogRepository.findTop5Keywords(LocalDateTime.now().minusDays(7));
    }

    /**
     * 최근 검색어 조회
     */
    public List<String> getRecentSearches(Long userId) {
        if (userId == null) {
            return List.of();
        }

        // 2. Implemented actual lookup using the repository
        return recentSearchRepository.findByUser_IdOrderBySearchedAtDesc(userId)
                .stream()
                .map(RecentSearch::getKeyword)
                .collect(Collectors.toList());
    }

    /**
     * 최근 검색어 개별 삭제
     */
    @Transactional
    public void deleteRecent(Long userId, String keyword) {
        if (userId == null || keyword == null) {
            return;
        }

        // 3. Replaced TODO with actual delete logic
        recentSearchRepository.deleteByUser_IdAndKeyword(userId, keyword);
        log.info("사용자 {}의 최근 검색어 '{}' 삭제 완료", userId, keyword);
    }

    /**
     * 내부 헬퍼: 최근 검색어 저장 로직
     */
    private void saveRecentSearch(Long userId, String keyword) {
        // 4. Replaced TODO with upsert logic for efficient updates
        recentSearchRepository.upsert(userId, keyword);
        log.debug("최근 검색어 저장 완료 - 유저: {}, 키워드: {}", userId, keyword);
    }
}