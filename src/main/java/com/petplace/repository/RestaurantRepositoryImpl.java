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
import java.util.Objects;
import java.util.stream.Collectors;

import static com.petplace.entity.QRestaurant.restaurant;

@RequiredArgsConstructor
public class RestaurantRepositoryImpl implements RestaurantRepositoryCustom {

    private final JPAQueryFactory queryFactory;

    @Override
    public Page<Restaurant> findByFilters(RestaurantFilterRequest cond, Pageable pageable) {

        // 💡 1. 필터 조건들을 하나의 배열로 정의 (regionEq -> regionIn 으로 변경)
        BooleanExpression[] conditions = {
                regionIn(cond.getRegions()), // 변경된 부분
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
        JPAQuery<Long> countQuery = queryFactory
                .select(restaurant.count())
                .from(restaurant)
                .where(conditions); // 동일한 조건 배열 전달

        // 4. Page 객체 반환
        return PageableExecutionUtils.getPage(content, pageable, countQuery::fetchOne);
    }

    // --- 💡 동적 조건 필터 메서드 수정 (IN 쿼리 적용) ---
    private BooleanExpression regionIn(List<String> regionStrs) {
        // 리스트가 비어있으면 조건 무시 (전체 조회)
        if (regionStrs == null || regionStrs.isEmpty()) return null;

        // String 리스트를 Enum(Region) 리스트로 안전하게 변환
        List<Region> validRegions = regionStrs.stream()
                .filter(StringUtils::hasText)
                .map(str -> {
                    try {
                        return Region.valueOf(str.toUpperCase());
                    } catch (IllegalArgumentException e) {
                        return null; // Enum에 없는 잘못된 값이 오면 무시
                    }
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());

        // 유효한 Enum 값이 없다면 조건 무시
        if (validRegions.isEmpty()) return null;

        // 여러 개의 지역을 IN 절로 검색
        return restaurant.region.in(validRegions);
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