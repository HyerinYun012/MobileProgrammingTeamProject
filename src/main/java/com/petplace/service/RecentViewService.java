package com.petplace.service;

import com.petplace.dto.response.RecentViewResponse;
import com.petplace.entity.Restaurant;
import com.petplace.exception.BusinessException;
import com.petplace.exception.ErrorCode; // 💡 ErrorCode import
import com.petplace.repository.RecentViewRepository;
import com.petplace.repository.RestaurantRepository; // 💡 검증을 위해 추가
import com.petplace.repository.UserRepository; // 💡 검증을 위해 추가
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecentViewService {

    private final RecentViewRepository recentViewRepo;
    private final UserRepository userRepo; // 💡 검증용 레포지토리
    private final RestaurantRepository restaurantRepo; // 💡 검증용 레포지토리
    private static final int RECENT_VIEW_LIMIT = 30;

    public Page<RecentViewResponse> getRecentViews(Long userId, Pageable pageable) {
        if (!userRepo.existsById(userId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        // 💡 Repository 호출 후 .map()을 사용하여 DTO 변환
        return recentViewRepo.findByUserId(userId, pageable)
                .map(recentView -> {
                    Restaurant restaurant = recentView.getRestaurant();
                    return new RecentViewResponse(
                            restaurant.getId(),
                            restaurant.getName(),
                            restaurant.getCategory() != null ? restaurant.getCategory().name() : null,
                            restaurant.getImageUrl(),
                            recentView.getCreatedAt()
                    );
                });
    }

    /**
     * 최근 본 장소 추가 (사용자 및 식당 존재 여부 검증 추가)
     */
    @Transactional
    public void addRecentView(Long userId, Long restaurantId) {
        if (!userRepo.existsById(userId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        if (!restaurantRepo.existsById(restaurantId)) {
            throw new BusinessException(ErrorCode.RESTAURANT_NOT_FOUND);
        }

        recentViewRepo.upsert(userId, restaurantId);
        recentViewRepo.deleteOldRecords(userId, RECENT_VIEW_LIMIT);
    }
}