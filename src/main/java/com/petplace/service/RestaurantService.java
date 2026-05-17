package com.petplace.service;

import com.petplace.dto.request.RestaurantFilterRequest;
import com.petplace.dto.request.RestaurantRequest;
import com.petplace.entity.Menu;
import com.petplace.entity.Restaurant;
import com.petplace.entity.User;
import com.petplace.exception.BusinessException;
import com.petplace.repository.RestaurantRepository;
import com.petplace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;

    /**
     * 내 주변 장소 조회
     */
    public List<Restaurant> findNearby(double lat, double lng, double radius) {
        return restaurantRepository.findNearby(lat, lng, radius);
    }

    /**
     * 조건 필터링 검색 (Querydsl)
     */
    public List<Restaurant> searchRestaurants(RestaurantFilterRequest condition) {
        return restaurantRepository.findByFilters(condition);
    }

    /**
     * 상세 정보 조회
     */
    public Restaurant getDetail(Long id) {
        return restaurantRepository.findById(id)
                .orElseThrow(() -> new BusinessException("해당 장소 정보를 찾을 수 없습니다."));
    }

    /**
     * 신규 장소 등록 (사장님 권한 검증 포함)
     */
    @Transactional
    public Long register(Long ownerId, RestaurantRequest req) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new BusinessException("존재하지 않는 사용자입니다."));

        if (owner.getRole() != User.Role.OWNER) {
            throw new BusinessException("장소 등록 권한이 없는 계정입니다.");
        }

        if (restaurantRepository.existsByBusinessNo(req.getBusinessNo())) {
            throw new BusinessException("이미 등록된 사업자 번호입니다.");
        }

        // DTO -> Entity 변환 및 연관관계 설정
        Restaurant restaurant = req.toEntity(ownerId);
        restaurant.setOwner(owner);

        return restaurantRepository.save(restaurant).getId();
    }

    /**
     * 장소 정보 수정 (소유권 검증 포함)
     */
    @Transactional
    public Long update(Long id, Long ownerId, RestaurantRequest req) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new BusinessException("해당 장소 정보를 찾을 수 없습니다."));

        // 인가 체크
        if (!restaurant.getOwnerId().equals(ownerId)) {
            throw new BusinessException("해당 장소의 수정 권한이 없습니다.");
        }

        // 1. 가게 기본 정보 및 영업시간 변경 (Dirty Checking)
        restaurant.update(req);

        // 2. 메뉴 정보 일괄 갱신 (기존 메뉴 제거 후 신규 데이터 바인딩)
        if (req.getMenus() != null) {
            List<Menu> newMenus = req.getMenus().stream()
                    .map(menuDto -> Menu.builder()
                            .name(menuDto.getName())
                            .price(menuDto.getPrice())
                            .description(menuDto.getDescription())
                            .restaurant(restaurant)
                            .build())
                    .toList();

            restaurant.updateMenus(newMenus);
        }

        return restaurant.getId();
    }
}