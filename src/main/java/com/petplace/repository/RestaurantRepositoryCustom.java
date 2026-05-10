package com.petplace.repository;

import com.petplace.dto.request.RestaurantFilterRequest;
import com.petplace.entity.Restaurant;
import java.util.List;

public interface RestaurantRepositoryCustom {
    List<Restaurant> findByFilters(RestaurantFilterRequest condition);
}