package com.petplace.service;

import com.petplace.dto.response.RestaurantResponse;
import com.petplace.entity.Restaurant;
import com.petplace.entity.RecentSearch;
import com.petplace.exception.BusinessException;
import com.petplace.exception.ErrorCode;
import com.petplace.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Set;
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
    private final BookmarkRepository bookmarkRepository;
    private final AsyncSearchLogService asyncSearchLogService; // 💡 비동기 서비스 주입

    /**
     * 키워드 통합 검색 처리 (북마크 반환 기능 추가 및 Response DTO 결합 변경)
     */
    @Transactional
    public Page<RestaurantResponse> search(String keyword, Long userId, Pageable pageable) {
        // 1. 유저 정보 검증 및 최근 검색어 기록 저장 처리
        if (userId != null) {
            userRepository.findById(userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
            saveRecentSearch(userId, keyword);
        }

        // 2. 비동기 검색어 통계 로그 적재 (수정: saveLogAsync 호출) 및 엔티티 결과 검색 (수정: findByNameContainingIgnoreCase 호출)
        asyncSearchLogService.saveLogAsync(keyword);
        Page<Restaurant> restaurantPage = restaurantRepository.findByNameContainingIgnoreCase(keyword, pageable);

        // 3. 로그인 사용자의 경우 한 번의 쿼리로 페이지 내 가게들의 북마크 여부 집계 일괄 추출
        Set<Long> bookmarkedRestaurantIds = Collections.emptySet();
        if (userId != null && !restaurantPage.isEmpty()) {
            List<Long> restaurantIds = restaurantPage.getContent().stream()
                    .map(Restaurant::getId)
                    .collect(Collectors.toList());
            bookmarkedRestaurantIds = bookmarkRepository.findRestaurantIdsByUserIdAndRestaurantIdIn(userId, restaurantIds);
        }

        // 4. 변환 파이프라인 구동하여 최종 DTO 반환 규격 준수 처리 (수정: DTO 내부 구조 변경에 맞춤)
        final Set<Long> finalBookmarkedIds = bookmarkedRestaurantIds;
        return restaurantPage.map(restaurant -> {
            boolean isBookmarked = finalBookmarkedIds.contains(restaurant.getId());
            return RestaurantResponse.from(restaurant, isBookmarked);
        });
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
        recentSearchRepository.upsert(userId, keyword);
        log.debug("최근 검색어 저장 완료 - 유저: {}, 키워드: {}", userId, keyword);
    }
}