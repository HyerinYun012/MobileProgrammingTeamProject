package com.petplace.service;

import com.petplace.dto.request.MenuRequest;
import com.petplace.dto.response.MenuResponse;
import com.petplace.entity.Menu;
import com.petplace.entity.Restaurant;
import com.petplace.exception.BusinessException;
import com.petplace.exception.ErrorCode; // 💡 import 추가
import com.petplace.repository.MenuRepository;
import com.petplace.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RestaurantMenuService {

    private final RestaurantRepository restaurantRepository;
    private final MenuRepository menuRepository;
    private final FileService fileService;

    /**
     * 메뉴 등록 (사장님 전용)
     * 🚀 [개선] 롤백 시 S3 업로드 파일 삭제 로직 추가
     */
    @Transactional
    public Long registerMenu(Long restaurantId, Long ownerId, MenuRequest req, MultipartFile imageFile) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESTAURANT_NOT_FOUND));

        if (restaurant.getOwner() == null || !restaurant.getOwner().getId().equals(ownerId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

        String imageUrl = null;
        List<String> uploadedFiles = new ArrayList<>(); // 롤백 대비 추적 리스트

        if (imageFile != null && !imageFile.isEmpty()) {
            imageUrl = fileService.uploadFile(imageFile);
            uploadedFiles.add(imageUrl);
        }

        // 트랜잭션 롤백 시 파일 삭제 동기화
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    uploadedFiles.forEach(fileService::deleteFile);
                }
            }
        });

        Menu menu = Menu.builder()
                .restaurant(restaurant)
                .name(req.getName())
                .price(req.getPrice())
                .description(req.getDescription())
                .imageUrl(imageUrl)
                .build();

        return menuRepository.save(menu).getId();
    }

    /**
     * 메뉴 수정 (S3 구버전 파일 삭제 연동 - 트랜잭션 동기화 적용)
     */
    @Transactional
    public void updateMenu(Long menuId, Long ownerId, MenuRequest req, MultipartFile newSpecFile) {
        // 💡 ErrorCode 적용
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MENU_NOT_FOUND));

        if (menu.getRestaurant().getOwner() == null || !menu.getRestaurant().getOwner().getId().equals(ownerId)) {
            // 💡 ErrorCode 적용
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

        String nextImageUrl = menu.getImageUrl();
        if (newSpecFile != null && !newSpecFile.isEmpty()) {
            nextImageUrl = fileService.uploadFile(newSpecFile);

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

        menu.updateMenuInfo(req.getName(), req.getPrice(), req.getDescription(), nextImageUrl);
    }

    /**
     * 메뉴 삭제 (S3 파일 완전 삭제 연동 - 트랜잭션 동기화 적용)
     */
    @Transactional
    public void deleteMenu(Long menuId, Long ownerId) {
        // 💡 ErrorCode 적용
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MENU_NOT_FOUND));

        if (menu.getRestaurant().getOwner() == null || !menu.getRestaurant().getOwner().getId().equals(ownerId)) {
            // 💡 ErrorCode 적용
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

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

    public Page<MenuResponse> getMenusByRestaurant(Long restaurantId, Pageable pageable) {
        // 💡 식당 존재 여부 확인
        restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESTAURANT_NOT_FOUND));

        // 💡 Repository에서 Page<Menu>를 받아 Page<MenuResponse>로 변환
        return menuRepository.findByRestaurantId(restaurantId, pageable)
                .map(MenuResponse::from);
    }
}