package com.petplace.service;

import com.petplace.dto.response.RecentViewResponse;
import com.petplace.entity.Restaurant;
import com.petplace.repository.RecentViewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecentViewService {

    private final RecentViewRepository recentViewRepo;
    private static final int RECENT_VIEW_LIMIT = 30;

    /**
     * 최근 본 장소 조회 (List<RecentViewResponse>로 정밀 가공)
     */
    public List<RecentViewResponse> getRecentViews(Long userId) {
        // ⚠️ [주의] 만약 RecentViewRepository의 쿼리 메서드 이름도 viewedAt을 바라보고 있다면
        // findTop10ByUserIdOrderByCreatedAtDesc 형태로 Repository단 명명 규칙도 함께 고쳐주셔야 합니다.
        return recentViewRepo.findTop10ByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(recentView -> {
                    Restaurant restaurant = recentView.getRestaurant();
                    return new RecentViewResponse(
                            restaurant.getId(),
                            restaurant.getName(),
                            restaurant.getCategory() != null ? restaurant.getCategory().name() : null,
                            restaurant.getImageUrl(),
                            // 🌟 [수정] 소멸된 recentView.getViewedAt() 대신
                            // 부모 클래스로부터 상속받은 전역 표준 Auditing 필드인 getCreatedAt()을 호출합니다.
                            recentView.getCreatedAt()
                    );
                })
                .toList();
    }

    /**
     * 최근 본 장소 추가
     */
    @Transactional
    public void addRecentView(Long userId, Long restaurantId) {
        recentViewRepo.upsert(userId, restaurantId);
        recentViewRepo.deleteOldRecords(userId, RECENT_VIEW_LIMIT);
    }
}