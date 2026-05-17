package com.petplace.service;

import com.petplace.entity.Menu;
import com.petplace.entity.Restaurant;
import com.petplace.exception.BusinessException;
import com.petplace.repository.MenuRepository;
import com.petplace.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 💡 기본 읽기 전용 성능 최적화
public class RestaurantMenuService {

    private final RestaurantRepository restaurantRepository;
    private final MenuRepository menuRepository;
    private final FileService fileService;

    /**
     * 메뉴 등록 (사장님 전용)
     */
    @Transactional
    public void registerMenu(Long restaurantId, Long ownerId, String name, int price, String description, String imageUrl) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new BusinessException("가게를 찾을 수 없습니다."));

        if (restaurant.getOwner() == null || !restaurant.getOwner().getId().equals(ownerId)) {
            throw new BusinessException("메뉴 등록 권한이 없습니다.");
        }

        // 🌟 [수정 완료] protected 기본 생성자 제약에 맞춰 @SuperBuilder 패턴으로 안전하게 생성합니다.
        Menu menu = Menu.builder()
                .restaurant(restaurant)
                .name(name)
                .price(price)
                .description(description)
                .imageUrl(imageUrl)
                .build();

        menuRepository.save(menu);
    }

    /**
     * 메뉴 수정 (S3 구버전 파일 삭제 연동 - 트랜잭션 동기화 적용)
     */
    @Transactional
    public void updateMenu(Long menuId, Long ownerId, String name, int price, String description, String imageUrl) {
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new BusinessException("메뉴를 찾을 수 없습니다."));

        if (menu.getRestaurant().getOwner() == null || !menu.getRestaurant().getOwner().getId().equals(ownerId)) {
            throw new BusinessException("메뉴 수정 권한이 없습니다.");
        }

        // DB 작업 성공(Commit)이 최종 확정되면, 그때 S3 파일을 삭제하도록 예약합니다.
        if (imageUrl != null && menu.getImageUrl() != null && !menu.getImageUrl().equals(imageUrl)) {
            String oldImageUrl = menu.getImageUrl();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    fileService.deleteFile(oldImageUrl);
                }
            });
        }

        // 🌟 [수정 완료] 파편화된 개별 Setter 호출을 폐쇄하고, Menu 엔티티의 캡슐화된 도메인 메서드를 호출합니다.
        menu.updateMenuInfo(name, price, description, imageUrl);
    }

    /**
     * 메뉴 삭제 (S3 파일 완전 삭제 연동 - 트랜잭션 동기화 적용)
     */
    @Transactional
    public void deleteMenu(Long menuId, Long ownerId) {
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new BusinessException("메뉴를 찾을 수 없습니다."));

        if (menu.getRestaurant().getOwner() == null || !menu.getRestaurant().getOwner().getId().equals(ownerId)) {
            throw new BusinessException("메뉴 삭제 권한이 없습니다.");
        }

        // 마찬가지로 DB 레코드가 완전히 정상 삭제(Commit)된 후에 S3 버킷 파일 제거
        if (menu.getImageUrl() != null && !menu.getImageUrl().isEmpty()) {
            String targetImageUrl = menu.getImageUrl();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    fileService.deleteFile(targetImageUrl);
                }
            });
        }

        menuRepository.delete(menu);
    }

    /**
     * 💡 특정 식당의 전체 메뉴 목록 조회 (전체 공개)
     */
    public List<Menu> getMenusByRestaurant(Long restaurantId) {
        restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new BusinessException("가게를 찾을 수 없습니다."));

        // 🌟 [가독성 개선] 스프링 데이터 JPA 컨벤션에 맞춰 메서드명의 언더바(_)를 제거한 양식으로 호출합니다.
        return menuRepository.findByRestaurantId(restaurantId);
    }
}