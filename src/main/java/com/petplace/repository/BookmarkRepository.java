package com.petplace.repository;
import com.petplace.entity.Bookmark;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;
public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {
    List<Bookmark> findByUser_IdOrderByCreatedAtDesc(Long userId);
    Optional<Bookmark> findByUser_IdAndRestaurant_Id(Long userId, Long restaurantId);
}
