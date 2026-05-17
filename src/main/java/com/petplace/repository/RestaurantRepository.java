package com.petplace.repository;

import com.petplace.entity.Restaurant;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Long>, RestaurantRepositoryCustom {

    /**
     * 사업자 번호 중복 여부 확인
     * @param businessNo 사업자 등록 번호 (000-00-00000)
     * @return 존재 여부
     */
    boolean existsByBusinessNo(String businessNo);

    /**
     * 이름 기반 검색 (대소문자 무시)
     */
    List<Restaurant> findByNameContainingIgnoreCase(String keyword);

    /**
     * 위치 기반 반경 내 장소 검색 (Haversine Formula 사용)
     * @param lat 기준 위도
     * @param lng 기준 경도
     * @param radius 검색 반경 (km)
     */
    @Query(value = "SELECT * FROM (" +
            "  SELECT *, " +
            "  (6371 * acos(cos(radians(:lat)) * cos(radians(latitude)) * " +
            "  cos(radians(longitude) - radians(:lng)) + " +
            "  sin(radians(:lat)) * sin(radians(latitude)))) AS distance " +
            "  FROM restaurants" +
            ") r " +
            "WHERE r.distance < :radius " +
            "ORDER BY r.distance", nativeQuery = true)
    List<Restaurant> findNearby(@Param("lat") double lat, @Param("lng") double lng, @Param("radius") double radius);
}