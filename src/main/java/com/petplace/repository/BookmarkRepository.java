package com.petplace.repository;

import com.petplace.entity.Bookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.Set;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    Page<Bookmark> findAllByUserId(Long userId, Pageable pageable);

    Optional<Bookmark> findByUserIdAndRestaurantId(Long userId, Long restaurantId);

    boolean existsByUserIdAndRestaurantId(Long userId, Long restaurantId);

    List<Bookmark> findAllByUserId(Long userId);

    @Query("SELECT b.restaurant.id FROM Bookmark b WHERE b.user.id = :userId AND b.restaurant.id IN :restaurantIds")
    Set<Long> findRestaurantIdsByUserIdAndRestaurantIdIn(@Param("userId") Long userId, @Param("restaurantIds") List<Long> restaurantIds);

    void deleteAllByRestaurantId(Long restaurantId);

    void deleteAllByUserId(Long userId);
}