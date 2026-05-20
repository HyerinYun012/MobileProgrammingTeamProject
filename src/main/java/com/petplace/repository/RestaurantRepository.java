package com.petplace.repository;

import com.petplace.entity.Restaurant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Long>, RestaurantRepositoryCustom {

    /**
     * 사업자 번호 중복 여부 확인
     * (단일 결과 반환이므로 페이징 불필요)
     */
    boolean existsByBusinessNo(String businessNo);

    /**
     * 💡 [페이징 적용] 이름 기반 검색 (대소문자 무시)
     * 많은 검색 결과가 나올 수 있으므로 Page<Restaurant>로 변경
     */
    Page<Restaurant> findByNameContainingIgnoreCase(String keyword, Pageable pageable);

    /**
     * 💡 [페이징 적용] 위치 기반 반경 내 장소 검색 (Haversine Formula 사용)
     * 1. Pageable 파라미터 추가
     * 2. countQuery 명시: 네이티브 쿼리에서 페이징을 하려면 전체 개수를 세는 쿼리가 필수입니다.
     */
    @Query(value = "SELECT * FROM (" +
            "  SELECT *, " +
            "  (6371 * acos(cos(radians(:lat)) * cos(radians(latitude)) * " +
            "  cos(radians(longitude) - radians(:lng)) + " +
            "  sin(radians(:lat)) * sin(radians(latitude)))) AS distance " +
            "  FROM restaurants" +
            ") r " +
            "WHERE r.distance < :radius",
            countQuery = "SELECT count(*) FROM (" +
                    "  SELECT (6371 * acos(cos(radians(:lat)) * cos(radians(latitude)) * " +
                    "  cos(radians(longitude) - radians(:lng)) + " +
                    "  sin(radians(:lat)) * sin(radians(latitude)))) AS distance " +
                    "  FROM restaurants" +
                    ") r " +
                    "WHERE r.distance < :radius",
            nativeQuery = true)
    Page<Restaurant> findNearby(@Param("lat") double lat,
                                @Param("lng") double lng,
                                @Param("radius") double radius,
                                Pageable pageable);
}