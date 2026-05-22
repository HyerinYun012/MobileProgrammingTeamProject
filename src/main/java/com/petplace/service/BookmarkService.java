package com.petplace.service;

import com.petplace.dto.response.BookmarkResponse;
import com.petplace.entity.Bookmark;
import com.petplace.entity.Restaurant;
import com.petplace.entity.User;
import com.petplace.exception.BusinessException;
import com.petplace.exception.ErrorCode;
import com.petplace.repository.BookmarkRepository;
import com.petplace.repository.RestaurantRepository;
import com.petplace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class BookmarkService {

    private final BookmarkRepository bookmarkRepo;
    private final UserRepository userRepo;
    private final RestaurantRepository restaurantRepo;


    public Set<Long> getBookmarkedRestaurantIds(Long userId) {
        if (userId == null) return Set.of();
        return bookmarkRepo.findAllByUserId(userId).stream()
                .map(bookmark -> bookmark.getRestaurant().getId())
                .collect(Collectors.toSet());
    }

    public Page<BookmarkResponse> getBookmarks(Long userId, Pageable pageable) {
        return bookmarkRepo.findAllByUserId(userId, pageable)
                .map(bookmark -> {
                    Restaurant restaurant = bookmark.getRestaurant();
                    return new BookmarkResponse(
                            restaurant.getId(),
                            restaurant.getName(),
                            restaurant.getCategory() != null ? restaurant.getCategory().name() : null,
                            restaurant.getAddress(),
                            restaurant.getImageUrl()
                    );
                });
    }

    /**
     * 북마크 토글 (생성/삭제)
     * 데이터 조회가 아닌 단일 개체 수정이므로 기존 방식 유지
     */
    @Transactional
    public boolean toggleBookmark(Long userId, Long restaurantId) {
        Optional<Bookmark> existing = bookmarkRepo.findByUserIdAndRestaurantId(userId, restaurantId);

        if (existing.isPresent()) {
            bookmarkRepo.delete(existing.get());
            return false;
        } else {
            User user = userRepo.findById(userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
            Restaurant restaurant = restaurantRepo.findById(restaurantId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.RESTAURANT_NOT_FOUND));

            Bookmark bookmark = Bookmark.createBookmark(user, restaurant);
            bookmarkRepo.save(bookmark);
            return true;
        }
    }
}