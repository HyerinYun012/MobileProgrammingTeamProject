package com.petplace.service;

import com.petplace.dto.response.RecentViewResponse;
import com.petplace.entity.RecentView;
import com.petplace.entity.Restaurant;
import com.petplace.exception.BusinessException;
import com.petplace.exception.ErrorCode;
import com.petplace.repository.BookmarkRepository; // 💡 북마크 조회를 위해 추가
import com.petplace.repository.RecentViewRepository;
import com.petplace.repository.RestaurantRepository;
import com.petplace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecentViewService {

    private final RecentViewRepository recentViewRepo;
    private final UserRepository userRepo;
    private final RestaurantRepository restaurantRepo;
    private final BookmarkRepository bookmarkRepo; // 💡 의존성 주입 추가
    private static final int RECENT_VIEW_LIMIT = 30;

    public Page<RecentViewResponse> getRecentViews(Long userId, Pageable pageable) {
        if (!userRepo.existsById(userId)) {
            throw new BusinessException(ErrorCode.USER_NOT_FOUND);
        }

        Page<RecentView> recentViews = recentViewRepo.findByUserId(userId, pageable);

        Set<Long> bookmarkedRestaurantIds = bookmarkRepo.findAllByUserId(userId).stream()
                .map(bookmark -> bookmark.getRestaurant().getId())
                .collect(Collectors.toSet());

        return recentViews.map(recentView -> {
            Restaurant restaurant = recentView.getRestaurant();

            RecentViewResponse response = new RecentViewResponse(
                    restaurant.getId(),
                    restaurant.getName(),
                    restaurant.getCategory() != null ? restaurant.getCategory().getDescription() : null,
                    restaurant.getImageUrl(),
                    recentView.getCreatedAt()
            );

            boolean isBookmarked = bookmarkedRestaurantIds.contains(restaurant.getId());
            response.setBookmarked(isBookmarked);

            return response;
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