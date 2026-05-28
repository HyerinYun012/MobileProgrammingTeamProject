package com.petplace.service;

import com.petplace.dto.request.MenuRequest;
import com.petplace.dto.response.MenuResponse;
import com.petplace.entity.Menu;
import com.petplace.entity.Restaurant;
import com.petplace.exception.BusinessException;
import com.petplace.exception.ErrorCode;
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
     * 1. 메뉴 등록 (사장님 전용)
     */
    @Transactional(rollbackFor = Exception.class)
    public Long registerMenu(Long restaurantId, Long ownerId, MenuRequest req, MultipartFile imageFile) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESTAURANT_NOT_FOUND));

        if (restaurant.getOwner() == null || !restaurant.getOwner().getId().equals(ownerId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

        // 💡 [추가] 사장님 계정 승인 여부 검증
        if (!restaurant.getOwner().isVerified()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_OWNER);
        }

        String imageUrl = null;
        final List<String> uploadedFiles = new ArrayList<>();

        if (imageFile != null && !imageFile.isEmpty()) {
            String tempUrl = fileService.uploadFile(imageFile);
            if (tempUrl != null) {
                imageUrl = tempUrl;
                uploadedFiles.add(tempUrl);
            }
        }

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
     * 2. 메뉴 수정
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateMenu(Long menuId, Long ownerId, MenuRequest req, MultipartFile newSpecFile) {
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MENU_NOT_FOUND));

        if (menu.getRestaurant().getOwner() == null || !menu.getRestaurant().getOwner().getId().equals(ownerId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

        // 💡 [추가] 사장님 계정 승인 여부 검증
        if (!menu.getRestaurant().getOwner().isVerified()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_OWNER);
        }

        final List<String> oldImageUrls = new ArrayList<>();
        final List<String> newlyUploadedFiles = new ArrayList<>();
        String nextImageUrl = menu.getImageUrl();

        if (newSpecFile != null && !newSpecFile.isEmpty()) {
            String tempUrl = fileService.uploadFile(newSpecFile);
            if (tempUrl != null) {
                nextImageUrl = tempUrl;
                newlyUploadedFiles.add(tempUrl);

                if (menu.getImageUrl() != null && !menu.getImageUrl().isEmpty()) {
                    oldImageUrls.add(menu.getImageUrl());
                }
            }
        }

        menu.updateMenuInfo(req.getName(), req.getPrice(), req.getDescription(), nextImageUrl);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (String imageUrl : oldImageUrls) {
                    fileService.deleteFile(imageUrl);
                }
            }
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    newlyUploadedFiles.forEach(fileService::deleteFile);
                }
            }
        });
    }

    /**
     * 3. 메뉴 삭제
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteMenu(Long menuId, Long ownerId) {
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MENU_NOT_FOUND));

        if (menu.getRestaurant().getOwner() == null || !menu.getRestaurant().getOwner().getId().equals(ownerId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

        // 💡 [추가] 사장님 계정 승인 여부 검증
        if (!menu.getRestaurant().getOwner().isVerified()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_OWNER);
        }

        if (menu.getImageUrl() != null && !menu.getImageUrl().isEmpty()) {
            final String targetImageUrl = menu.getImageUrl();
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
     * 4. 특정 식당의 전체 메뉴 목록 조회
     */
    public Page<MenuResponse> getMenusByRestaurant(Long restaurantId, Pageable pageable) {
        restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESTAURANT_NOT_FOUND));

        return menuRepository.findByRestaurantId(restaurantId, pageable)
                .map(MenuResponse::from);
    }
}