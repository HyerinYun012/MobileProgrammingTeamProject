package com.petplace.service;
import com.petplace.dto.request.*;
import com.petplace.entity.*;
import com.petplace.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
import java.util.Map;
@Service @RequiredArgsConstructor @Transactional
public class MyPageService {
    private final UserRepository userRepo;
    private final BookmarkRepository bookmarkRepo;
    private final RecentViewRepository recentViewRepo;
    private final ReviewRepository reviewRepo;
    private final PetRepository petRepo;

    public User getProfile(Long userId) { return userRepo.findById(userId).orElseThrow(); }
    public void updateProfile(Long userId, UpdateProfileRequest req) {
        User u = getProfile(userId); u.setNickname(req.getNickname()); u.setEmail(req.getEmail());
        u.setPhone(req.getPhone()); if (req.getProfileUrl() != null) u.setProfileUrl(req.getProfileUrl());
        userRepo.save(u);
    }
    public List<Bookmark> getBookmarks(Long userId) { return bookmarkRepo.findByUser_IdOrderByCreatedAtDesc(userId); }
    public Map<String, Object> toggleBookmark(Long userId, Long restaurantId) {
        return bookmarkRepo.findByUser_IdAndRestaurant_Id(userId, restaurantId)
            .map(b -> { bookmarkRepo.delete(b); return Map.of("bookmarked", (Object) false); })
            .orElseGet(() -> {
                Bookmark bm = new Bookmark(); bm.setUser(new User(userId));
                bm.setRestaurant(new Restaurant(restaurantId)); bookmarkRepo.save(bm);
                return Map.of("bookmarked", (Object) true);
            });
    }
    public List<RecentView> getRecentViews(Long userId) { return recentViewRepo.findTop10ByUser_IdOrderByViewedAtDesc(userId); }
    public void addRecentView(Long userId, Long restaurantId) { recentViewRepo.upsert(userId, restaurantId); }
    public List<Review> getMyReviews(Long userId) { return reviewRepo.findByUser_IdOrderByCreatedAtDesc(userId); }
    public List<Pet> getPets(Long userId) { return petRepo.findByUser_Id(userId); }
    public Pet addPet(Long userId, PetRequest req) {
        Pet p = new Pet(); p.setUser(new User(userId)); p.setName(req.getName());
        p.setBirth(req.getBirth()); p.setBreed(req.getBreed()); p.setImageUrl(req.getImageUrl());
        return petRepo.save(p);
    }
    public Pet updatePet(Long petId, PetRequest req) {
        Pet p = petRepo.findById(petId).orElseThrow(); p.setName(req.getName());
        p.setBirth(req.getBirth()); p.setBreed(req.getBreed());
        if (req.getImageUrl() != null) p.setImageUrl(req.getImageUrl());
        return petRepo.save(p);
    }
}
