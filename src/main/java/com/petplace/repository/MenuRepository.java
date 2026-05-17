package com.petplace.repository;

import com.petplace.entity.Menu;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MenuRepository extends JpaRepository<Menu, Long> {
    // Restaurant 엔티티 내부의 id 필드를 참조하도록 _Id 매핑 규칙을 적용합니다.
    List<Menu> findByRestaurantId(Long restaurantId);
}