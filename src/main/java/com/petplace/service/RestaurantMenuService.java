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
     * ★ 규칙 4 적용: null 반환 예외 방어 가드 장치 및 롤백 S3 청소 공정 완비
     */
    @Transactional(rollbackFor = Exception.class)
    public Long registerMenu(Long restaurantId, Long ownerId, MenuRequest req, MultipartFile imageFile) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESTAURANT_NOT_FOUND));

        if (restaurant.getOwner() == null || !restaurant.getOwner().getId().equals(ownerId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

        String imageUrl = null;
        // 💡 자바 컴파일 에러를 방지하기 위해 final 선언 유지 (참조 주소 고정)
        final List<String> uploadedFiles = new ArrayList<>();

        // 💡 규칙 4: 비어있는 파일 유입 방어 코드 추가
        if (imageFile != null && !imageFile.isEmpty()) {
            String tempUrl = fileService.uploadFile(imageFile);

            // 🌟 [핵심 null 반환 예외 방어]: S3가 확실히 URL 주소를 리턴했을 때만 변수 가공 및 추적 관리 시작
            if (tempUrl != null) {
                imageUrl = tempUrl;
                uploadedFiles.add(tempUrl);
            }
        }

        // ★ 규칙 4: 트랜잭션 롤백-S3 동기화 대책 동형 매핑
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                // 비즈니스 로직 도중 예외가 나서 트랜잭션이 최종 롤백되었다면 방금 업로드했던 S3 신규 유령 파일 추적 삭제
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
     * 2. 메뉴 수정 (S3 구버전 파일 삭제 연동 및 새버전 롤백 양방향 트랜잭션 동기화 완성)
     * ★ 규칙 4 적용: 커밋(구버전 삭제) 및 롤백(수정 실패 시 새버전 철회) 양방향 완벽 동기화
     */
    @Transactional(rollbackFor = Exception.class)
    public void updateMenu(Long menuId, Long ownerId, MenuRequest req, MultipartFile newSpecFile) {
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MENU_NOT_FOUND));

        if (menu.getRestaurant().getOwner() == null || !menu.getRestaurant().getOwner().getId().equals(ownerId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

        // Effectively Final 구조를 유지하기 위해 인스턴스화 뼈대 리스트 선언
        final List<String> oldImageUrls = new ArrayList<>();
        final List<String> newlyUploadedFiles = new ArrayList<>();

        String nextImageUrl = menu.getImageUrl();

        // 💡 규칙 4: 비어있는 파일 유입 방어 코드 추가
        if (newSpecFile != null && !newSpecFile.isEmpty()) {
            String tempUrl = fileService.uploadFile(newSpecFile);

            // 🌟 [핵심 null 반환 예외 방어]: S3가 확실하게 새 이미지 주소를 반환했을 때만 연동 처리
            if (tempUrl != null) {
                nextImageUrl = tempUrl;
                newlyUploadedFiles.add(tempUrl); // 롤백 발생 시 회수할 대상

                // 기존 구버전 파일이 존재했다면 백업 등록 (성공 시 삭제할 대상)
                if (menu.getImageUrl() != null && !menu.getImageUrl().isEmpty()) {
                    oldImageUrls.add(menu.getImageUrl());
                }
            }
        }

        // 엔티티 정보 갱신
        menu.updateMenuInfo(req.getName(), req.getPrice(), req.getDescription(), nextImageUrl);

        // ★ 규칙 4: 수정용 커밋-롤백 양방향 동기화 대책 메커니즘 탑재 (유사 final 에러 완벽 해결)
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // 성공 시: 트랜잭션이 성공적으로 완결되었으므로 구버전 S3 이미지 파괴 (용량 최적화)
                for (String imageUrl : oldImageUrls) {
                    fileService.deleteFile(imageUrl);
                }
            }

            @Override
            public void afterCompletion(int status) {
                // 실패 시: 수정 과정에서 에러가 터져 롤백되었다면 이번 수정 시도로 잠깐 올라갔던 새 S3 파일만 즉시 삭제 회수
                if (status == STATUS_ROLLED_BACK) {
                    newlyUploadedFiles.forEach(fileService::deleteFile);
                }
            }
        });
    }

    /**
     * 3. 메뉴 삭제 (S3 파일 완전 삭제 연동 - 트랜잭션 동기화 적용)
     */
    @Transactional(rollbackFor = Exception.class)
    public void deleteMenu(Long menuId, Long ownerId) {
        Menu menu = menuRepository.findById(menuId)
                .orElseThrow(() -> new BusinessException(ErrorCode.MENU_NOT_FOUND));

        if (menu.getRestaurant().getOwner() == null || !menu.getRestaurant().getOwner().getId().equals(ownerId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

        if (menu.getImageUrl() != null && !menu.getImageUrl().isEmpty()) {
            final String targetImageUrl = menu.getImageUrl(); // 내부 클래스 전달을 위한 final 상수 처리

            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    // 삭제가 최종 DB에 반영 확정(Commit)되었을 때만 S3 물리 파일 삭제
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