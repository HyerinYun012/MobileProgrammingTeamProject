package com.petplace.service;

import com.petplace.entity.Restaurant;
import com.petplace.entity.RecentSearch;
import com.petplace.exception.BusinessException;
import com.petplace.exception.ErrorCode;
import com.petplace.repository.RestaurantRepository;
import com.petplace.repository.RecentSearchRepository;
import com.petplace.repository.SearchLogRepository;
import com.petplace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
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

    private final RestaurantRepository restaurantRepository;
    private final SearchLogRepository searchLogRepository;
    private final RecentSearchRepository recentSearchRepository;
    private final UserRepository userRepository;
    private final AsyncSearchLogService asyncSearchLogService; // 💡 비동기 서비스 주입

    /**
     * 키워드 통합 검색 (로그 저장 및 최근 검색어 반영)
     */
    @Transactional
    public Page<Restaurant> search(String keyword, Long userId, Pageable pageable) { // 1. Pageable 파라미터 추가
        if (keyword == null || keyword.trim().isEmpty()) {
            return Page.empty(); // 2. 빈 Page 반환
        }

        String trimmedKeyword = keyword.trim();

        // 비동기 로그 저장
        asyncSearchLogService.saveLogAsync(trimmedKeyword);

        // 최근 검색어 저장
        if (userId != null) {
            saveRecentSearch(userId, trimmedKeyword);
        }

        // 3. 반환 타입이 Page<Restaurant>이므로 그대로 리턴
        return restaurantRepository.findByNameContainingIgnoreCase(trimmedKeyword, pageable);
    }

    /**
     * 인기 검색어 TOP 5 조회
     */
    public List<String> getPopularKeywords() {
        // 인스턴스인 searchLogRepository를 통해 호출
        return searchLogRepository.findTop5Keywords(LocalDateTime.now().minusDays(7));
    }

    /**
     * 최근 검색어 조회 (최대 5개)
     */
    public List<String> getRecentSearches(Long userId) {
        if (userId == null) {
            return List.of();
        }

        if (!userRepository.existsById(userId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 1. Pageable을 사용하여 상위 5개(0페이지, 사이즈 5) 가져오기
        // 2. RecentSearch 엔티티 리스트를 가져온 후 keyword만 추출
        return recentSearchRepository.findByUser_Id(userId, Pageable.ofSize(5))
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

        if (!userRepository.existsById(userId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        recentSearchRepository.deleteByUser_IdAndKeyword(userId, keyword);
        log.info("사용자 {}의 최근 검색어 '{}' 삭제 완료", userId, keyword);
    }

    /**
     * 내부 헬퍼: 최근 검색어 저장 로직
     */
    private void saveRecentSearch(Long userId, String keyword) {
        if (!userRepository.existsById(userId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        recentSearchRepository.upsert(userId, keyword);
        log.debug("최근 검색어 저장 완료 - 유저: {}, 키워드: {}", userId, keyword);
    }
}