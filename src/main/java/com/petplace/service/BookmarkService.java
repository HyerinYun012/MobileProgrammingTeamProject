package com.petplace.service;

import com.petplace.dto.response.BookmarkResponse; // 💡 추가
import com.petplace.entity.Bookmark;
import com.petplace.entity.Restaurant;
import com.petplace.entity.User;
import com.petplace.exception.BusinessException;
import com.petplace.repository.BookmarkRepository;
import com.petplace.repository.RestaurantRepository;
import com.petplace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookmarkService {

    private final BookmarkRepository bookmarkRepo;
    private final UserRepository userRepo;
    private final RestaurantRepository restaurantRepo;

    /**
     * 북마크 목록 조회 (List<?> 대신 구체적인 List<BookmarkResponse>로 가공)
     */
    public List<BookmarkResponse> getBookmarks(Long userId) {
        return bookmarkRepo.findAllByUserIdOrderByCreatedAtDesc(userId).stream()
                .map(bookmark -> {
                    Restaurant restaurant = bookmark.getRestaurant();
                    return new BookmarkResponse(
                            restaurant.getId(),
                            restaurant.getName(),
                            restaurant.getCategory() != null ? restaurant.getCategory().name() : null,
                            restaurant.getAddress(),
                            restaurant.getImageUrl()
                    );
                })
                .toList();
    }

    /**
     * 북마크 토글 (추가/취소)
     */
    @Transactional
    public boolean toggleBookmark(Long userId, Long restaurantId) {
        Optional<Bookmark> existing = bookmarkRepo.findByUserIdAndRestaurantId(userId, restaurantId);

        if (existing.isPresent()) {
            bookmarkRepo.delete(existing.get());
            return false;
        } else {
            User user = userRepo.findById(userId)
                    .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다."));
            Restaurant restaurant = restaurantRepo.findById(restaurantId)
                    .orElseThrow(() -> new BusinessException("장소를 찾을 수 없습니다."));

            Bookmark bookmark = new Bookmark();
            bookmark.setUser(user);
            bookmark.setRestaurant(restaurant);
            bookmarkRepo.save(bookmark);
            return true;
        }
    }
}