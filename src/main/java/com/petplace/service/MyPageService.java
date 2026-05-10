package com.petplace.service;

import com.petplace.dto.request.PetRequest;
import com.petplace.dto.request.UpdateProfileRequest;
import com.petplace.entity.*;
import com.petplace.exception.BusinessException;
import com.petplace.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MyPageService {

    private final UserRepository userRepo;
    private final BookmarkRepository bookmarkRepo;
    private final RecentViewRepository recentViewRepo;
    private final ReviewRepository reviewRepo;
    private final PetRepository petRepo;
    private final RestaurantRepository restaurantRepo; // 추가

    private static final int RECENT_VIEW_LIMIT = 30;

    /**
     * 1. 프로필 조회 (컨트롤러 에러 해결)
     */
    public User getProfile(Long userId) {
        return userRepo.findById(userId)
                .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다."));
    }

    /**
     * 2. 프로필 수정
     */
    @Transactional
    public void updateProfile(Long userId, UpdateProfileRequest req) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다."));

        user.updateProfile(req.getNickname(), req.getProfileUrl());
        user.setEmail(req.getEmail());
        user.setPhone(req.getPhone());
    }

    /**
     * 3. 북마크 목록 조회
     */
    public List<?> getBookmarks(Long userId) {
        return bookmarkRepo.findAllByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * 4. 북마크 토글 (컨트롤러 에러 해결)
     */
    @Transactional
    public boolean toggleBookmark(Long userId, Long restaurantId) {
        // 이미 북마크했는지 확인 (Repository에 해당 메서드 선언 필요)
        Optional<Bookmark> existing = bookmarkRepo.findByUserIdAndRestaurantId(userId, restaurantId);

        if (existing.isPresent()) {
            bookmarkRepo.delete(existing.get());
            return false; // 북마크 취소
        } else {
            User user = userRepo.findById(userId)
                    .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다."));
            Restaurant restaurant = restaurantRepo.findById(restaurantId)
                    .orElseThrow(() -> new BusinessException("장소를 찾을 수 없습니다."));

            Bookmark bookmark = new Bookmark();
            bookmark.setUser(user);
            bookmark.setRestaurant(restaurant);
            bookmarkRepo.save(bookmark);
            return true; // 북마크 추가
        }
    }

    /**
     * 5. 최근 본 장소 조회
     */
    public List<?> getRecentViews(Long userId) {
        return recentViewRepo.findTop10ByUserIdOrderByViewedAtDesc(userId);
    }

    /**
     * 6. 최근 본 장소 추가 (Upsert 최적화)
     */
    @Transactional
    public void addRecentView(Long userId, Long restaurantId) {
        recentViewRepo.upsert(userId, restaurantId);
        recentViewRepo.deleteOldRecords(userId, RECENT_VIEW_LIMIT);
    }

    /**
     * 7. 내 리뷰 목록 조회
     */
    public List<?> getMyReviews(Long userId) {
        return reviewRepo.findAllByUserIdOrderByCreatedAtDesc(userId);
    }

    /**
     * 8. 반려동물 목록 조회
     */
    public List<?> getPets(Long userId) {
        return petRepo.findAllByUserId(userId);
    }

    /**
     * 9. 반려동물 추가
     */
    @Transactional
    public Pet addPet(Long userId, PetRequest req) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다."));

        Pet pet = new Pet();
        pet.setUser(user);
        pet.setName(req.getName());
        pet.setBreed(req.getBreed());
        pet.setBirth(req.getBirth());
        // 필요한 다른 필드 셋팅 (birthDate 등)

        return petRepo.save(pet);
    }

    /**
     * 10. 반려동물 정보 수정 (소유권 검증 및 엔티티 메서드 활용)
     */
    @Transactional
    public Pet updatePet(Long userId, Long petId, PetRequest req) {
        // 1. 반려동물 존재 여부 확인
        Pet pet = petRepo.findById(petId)
                .orElseThrow(() -> new BusinessException("반려동물 정보를 찾을 수 없습니다."));

        // 2. [중요] 소유권 검증: 현재 로그인한 유저가 이 반려동물의 주인이 맞는지 확인
        if (!pet.getUser().getId().equals(userId)) {
            throw new BusinessException("해당 반려동물 정보에 대한 수정 권한이 없습니다.");
        }

        // 3. Pet 엔티티의 비즈니스 로직을 활용하여 정보 업데이트
        // 개별 필드를 일일이 set 하지 않고, 엔티티 내부에서 처리하여 Dirty Checking 유도
        pet.updateInfo(req);

        return pet; // @Transactional에 의해 변경사항이 DB에 자동 반영됨
    }
}