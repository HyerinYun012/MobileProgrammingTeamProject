package com.petplace.service;

import com.petplace.dto.request.MenuRequest; // 💡 추가
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
import org.springframework.web.multipart.MultipartFile; // 💡 추가

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 기본 읽기 전용 성능 최적화
public class RestaurantMenuService {

    private final RestaurantRepository restaurantRepository;
    private final MenuRepository menuRepository;
    private final FileService fileService;

    /**
     * 메뉴 등록 (사장님 전용)
     */
    @Transactional
    public Long registerMenu(Long restaurantId, Long ownerId, MenuRequest req, MultipartFile imageFile) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new BusinessException("가게를 찾을 수 없습니다."));

        if (restaurant.getOwner() == null || !restaurant.getOwner().getId().equals(ownerId)) {
            throw new BusinessException("메뉴 등록 권한이 없습니다.");
        }

        // 💡 물리 파일 아웃소싱 체계 연동 (FileService가 Checked Exception을 내부 래핑하여 throws 문맥 제거됨)
        String uploadedImageUrl = fileService.uploadFile(imageFile);

        Menu menu = Menu.builder()
                .restaurant(restaurant)
                .name(req.getName())
                .price(req.getPrice())
                .description(req.getDescription())
                .imageUrl(uploadedImageUrl) // S3에 업로드되어 발급된 URL 할당
                .build();

        Menu savedMenu = menuRepository.save(menu);
        return savedMenu.getId();
    }

    /**
     * 메뉴 수정 (S3 구버전 파일 삭제 연동 - 트랜잭션 동기화 적용)
     */
    @Transactional
    public void updateMenu(Long menuId, Long ownerId, MenuRequest req, MultipartFile newSpecFile) {
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new BusinessException("메뉴를 찾을 수 없습니다."));

        if (menu.getRestaurant().getOwner() == null || !menu.getRestaurant().getOwner().getId().equals(ownerId)) {
            throw new BusinessException("메뉴 수정 권한이 없습니다.");
        }

        // 1. 새로 들어온 파일이 있다면 S3 업로드 먼저 선행
        String nextImageUrl = menu.getImageUrl();
        if (newSpecFile != null && !newSpecFile.isEmpty()) {
            nextImageUrl = fileService.uploadFile(newSpecFile);

            // 2. 업로드가 정상 수행되었고 기존 이미지가 존재했다면 커밋 완료 시점(afterCommit)에 제거 예약
            if (menu.getImageUrl() != null) {
                String oldImageUrl = menu.getImageUrl();
                TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                    @Override
                    public void afterCommit() {
                        fileService.deleteFile(oldImageUrl);
                    }
                });
            }
        }

        // 3. 엔티티 도메인 비즈니스 메서드 호출하여 원자적(Atomic) 데이터 교체
        menu.updateMenuInfo(req.getName(), req.getPrice(), req.getDescription(), nextImageUrl);
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

        // DB 레코드가 완전히 정상 삭제(Commit)된 후에 S3 버킷 파일 제거
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
     * 특정 식당의 전체 메뉴 목록 조회 (전체 공개)
     */
    public List<Menu> getMenusByRestaurant(Long restaurantId) {
        restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new BusinessException("가게를 찾을 수 없습니다."));

        return menuRepository.findByRestaurantId(restaurantId);
    }
}