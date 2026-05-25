package com.petplace.repository;
import com.petplace.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
public interface ReviewRepository extends JpaRepository<Review, Long> {
    List<Review> findByRestaurant_IdOrderByCreatedAtDesc(Long restaurantId);
    List<Review> findByUser_IdOrderByCreatedAtDesc(Long userId);
}
