package com.petplace.repository;

import com.petplace.dto.request.RestaurantFilterRequest;
import com.petplace.entity.Restaurant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface RestaurantRepositoryCustom {

    /**
     * 💡 [페이징 적용]
     * 필터 조건에 따른 식당 검색 결과를 페이징 처리하여 조회합니다.
     * * @param condition 검색 필터 조건 (지역, 카테고리 등)
     * @param pageable 페이징 정보 (페이지 번호, 사이즈, 정렬)
     * @return Page<Restaurant> 페이징된 식당 목록
     */
    Page<Restaurant> findByFilters(RestaurantFilterRequest condition, Pageable pageable);
}