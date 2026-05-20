package com.petplace.repository;

import com.petplace.dto.request.RestaurantFilterRequest;
import com.petplace.entity.Restaurant;
import com.petplace.entity.Restaurant.Region;
import com.querydsl.core.types.dsl.BooleanExpression;
import com.querydsl.jpa.impl.JPAQuery;
import com.querydsl.jpa.impl.JPAQueryFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.support.PageableExecutionUtils;
import org.springframework.util.StringUtils;

import java.util.List;

import static com.petplace.entity.QRestaurant.restaurant;

@RequiredArgsConstructor
public class RestaurantRepositoryImpl implements RestaurantRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Restaurant> findByFilters(RestaurantFilterRequest cond, Pageable pageable) {

        // 💡 1. 필터 조건들을 하나의 배열로 정의 (중복 제거 및 가독성 향상)
        BooleanExpression[] conditions = {
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
        };

        // 2. 데이터 조회 쿼리 (페이징 적용)
        List<Restaurant> content = queryFactory
                .selectFrom(restaurant)
                .where(conditions) // 조건 배열 전달
                .offset(pageable.getOffset())
                .limit(pageable.getPageSize())
                .orderBy(restaurant.id.desc()) // 필요시 정렬 기준 추가
                .fetch();

        // 3. 카운트 쿼리 (전체 데이터 개수 계산)
        // 💡 PageableExecutionUtils가 이 카운트 쿼리를 효율적으로 실행합니다.
        JPAQuery<Long> countQuery = queryFactory
                .select(restaurant.count())
                .from(restaurant)
                .where(conditions); // 동일한 조건 배열 전달

        // 4. Page 객체 반환
        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    // --- 동적 조건 필터 메서드 ---
    private BooleanExpression regionEq(String regionStr) {
        if (!StringUtils.hasText(regionStr)) return null;
        try {
            return restaurant.region.eq(Region.valueOf(regionStr.toUpperCase()));
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private BooleanExpression hasParkingEq(Boolean val) { return val != null ? restaurant.hasParking.eq(val) : null; }
    private BooleanExpression hasRestroomEq(Boolean val) { return val != null ? restaurant.hasRestroom.eq(val) : null; }
    private BooleanExpression allowSmallEq(Boolean val) { return val != null ? restaurant.allowSmall.eq(val) : null; }
    private BooleanExpression allowMediumEq(Boolean val) { return val != null ? restaurant.allowMedium.eq(val) : null; }
    private BooleanExpression allowLargeEq(Boolean val) { return val != null ? restaurant.allowLarge.eq(val) : null; }
    private BooleanExpression hasFenceEq(Boolean val) { return val != null ? restaurant.hasFence.eq(val) : null; }
    private BooleanExpression hasArtificialGrassEq(Boolean val) { return val != null ? restaurant.hasArtificialGrass.eq(val) : null; }
    private BooleanExpression hasNaturalGrassEq(Boolean val) { return val != null ? restaurant.hasNaturalGrass.eq(val) : null; }
    private BooleanExpression hasSnackEq(Boolean val) { return val != null ? restaurant.hasSnack.eq(val) : null; }
    private BooleanExpression hasIndoorEq(Boolean val) { return val != null ? restaurant.hasIndoor.eq(val) : null; }
    private BooleanExpression hasOutdoorEq(Boolean val) { return val != null ? restaurant.hasOutdoor.eq(val) : null; }
}