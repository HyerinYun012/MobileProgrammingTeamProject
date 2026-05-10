package com.petplace.service;
import com.petplace.entity.*;
import com.petplace.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;
@Service @RequiredArgsConstructor @Transactional
public class SearchService {
    private final RestaurantRepository restaurantRepo;
    private final RecentSearchRepository recentSearchRepo;
    private final SearchLogRepository searchLogRepo;

    public List<Restaurant> search(String keyword, Long userId) {
        recentSearchRepo.upsert(userId, keyword);
        SearchLog log = new SearchLog(); log.setKeyword(keyword); searchLogRepo.save(log);
        return restaurantRepo.findByNameContainingIgnoreCase(keyword);
    }
    public List<RecentSearch> getRecentSearches(Long userId) { return recentSearchRepo.findByUser_IdOrderBySearchedAtDesc(userId); }
    public void deleteRecent(Long userId, String keyword) { recentSearchRepo.deleteByUser_IdAndKeyword(userId, keyword); }
    public List<String> getRecommendKeywords() { return searchLogRepo.findTop5Keywords(); }
}
