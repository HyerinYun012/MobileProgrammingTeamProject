package com.petplace.repository;

import com.petplace.dto.request.RestaurantFilterRequest;
import com.petplace.entity.Restaurant;
import com.petplace.entity.Restaurant.Region;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.util.StringUtils;

import java.util.List;

import static com.petplace.entity.QRestaurant.restaurant; // Q클래스 static import

@RequiredArgsConstructor
public class RestaurantRepositoryImpl implements RestaurantRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public List<Restaurant> findByFilters(RestaurantFilterRequest cond) {
        return queryFactory
                .selectFrom(restaurant)
                .where(
                        regionEq(cond.getRegion()),
                        hasParkingEq(cond.getHasParking()),
                        hasRestroomEq(cond.getHasRestroom()),
                        allowSmallEq(cond.getAllowSmall()),
                        allowMediumEq(cond.getAllowMedium()),
                        allowLargeEq(cond.getAllowLarge()),
                        hasFenceEq(cond.getHasFence()),
                        hasArtificialGrassEq(cond.getHasArtificialGrass()),
                        hasNaturalGrassEq(cond.getHasNaturalGrass()),
                        hasSnackEq(cond.getHasSnack()),
                        hasIndoorEq(cond.getHasIndoor()),
                        hasOutdoorEq(cond.getHasOutdoor())
                )
                .fetch();
    }

    // --- 동적 조건 조각들 (수정 반영) ---

    private BooleanExpression regionEq(String regionStr) {
        if (!StringUtils.hasText(regionStr)) return null;
        try {
            // 대문자로 변환하여 Enum 매칭 안전성 확보
            return restaurant.region.eq(Region.valueOf(regionStr.toUpperCase()));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    // Boolean 필터: null이 아닐 때만 해당 값(true/false)과 일치하는지 확인
    private BooleanExpression hasParkingEq(Boolean val) {
        return val != null ? restaurant.hasParking.eq(val) : null;
    }

    private BooleanExpression hasRestroomEq(Boolean val) {
        return val != null ? restaurant.hasRestroom.eq(val) : null;
    }

    private BooleanExpression allowSmallEq(Boolean val) {
        return val != null ? restaurant.allowSmall.eq(val) : null;
    }

    private BooleanExpression allowMediumEq(Boolean val) {
        return val != null ? restaurant.allowMedium.eq(val) : null;
    }

    private BooleanExpression allowLargeEq(Boolean val) {
        return val != null ? restaurant.allowLarge.eq(val) : null;
    }

    private BooleanExpression hasFenceEq(Boolean val) {
        return val != null ? restaurant.hasFence.eq(val) : null;
    }

    private BooleanExpression hasArtificialGrassEq(Boolean val) {
        return val != null ? restaurant.hasArtificialGrass.eq(val) : null;
    }

    private BooleanExpression hasNaturalGrassEq(Boolean val) {
        return val != null ? restaurant.hasNaturalGrass.eq(val) : null;
    }

    private BooleanExpression hasSnackEq(Boolean val) {
        return val != null ? restaurant.hasSnack.eq(val) : null;
    }

    private BooleanExpression hasIndoorEq(Boolean val) {
        return val != null ? restaurant.hasIndoor.eq(val) : null;
    }

    private BooleanExpression hasOutdoorEq(Boolean val) {
        return val != null ? restaurant.hasOutdoor.eq(val) : null;
    }
}