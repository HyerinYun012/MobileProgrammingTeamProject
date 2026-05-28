package com.petplace.repository;

import com.petplace.entity.Restaurant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List; // 💡 List 임포트 추가

@Repository
public interface RestaurantRepository extends JpaRepository<Restaurant, Long>, RestaurantRepositoryCustom {

    /**
     * 사장님 ID로 등록된 모든 사업장(식당/카페) 목록 조회
     * 특정 사장님의 승인을 진행할 때 제출한 모든 사업자 정보를 확인하기 위해 사용됩니다.
     */
    List<Restaurant> findAllByOwnerId(Long ownerId);

    /**
     * 사업자 번호 중복 여부 확인 (신규 등록용)
     */
    boolean existsByBusinessNo(String businessNo);

    /**
     * [수정용] 자기 자신을 제외하고 사업자 번호 중복 확인
     */
    boolean existsByBusinessNoAndIdNot(String businessNo, Long id);

    /**
     * [페이징 적용] 이름 기반 검색 (대소문자 무시)
     * 많은 검색 결과가 나올 수 있으므로 Page<Restaurant>로 변경
     */
    Page<Restaurant> findByNameContainingIgnoreCase(String keyword, Pageable pageable);

    /**
     * [페이징 적용] 위치 기반 반경 내 장소 검색 (Haversine Formula 사용 및 거리순 정렬 완료)
     */
    @Query(value = "SELECT * FROM (" +
            "  SELECT *, " +
            "  (6371 * acos(cos(radians(:lat)) * cos(radians(latitude)) * " +
            "  cos(radians(longitude) - radians(:lng)) + " +
            "  sin(radians(:lat)) * sin(radians(latitude)))) AS distance " +
            "  FROM restaurants" +
            ") r " +
            "WHERE r.distance < :radius " +
            "ORDER BY r.distance ASC",
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