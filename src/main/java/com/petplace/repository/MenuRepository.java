package com.petplace.repository;

import com.petplace.entity.Menu;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface MenuRepository extends JpaRepository<Menu, Long> {

    /**
     * 💡 [페이징 적용]
     * 기존 List<Menu>를 Page<Menu>로 변경하여 OOM(메모리 고갈) 위험을 제거했습니다.
     * * @param restaurantId 조회할 식당 ID
     * @param pageable 페이징 및 정렬 정보 (요청 시 PageRequest.of() 사용)
     * @return Page<Menu> 페이징 처리된 메뉴 목록
     */
    Page<Menu> findByRestaurantId(Long restaurantId, Pageable pageable);
}