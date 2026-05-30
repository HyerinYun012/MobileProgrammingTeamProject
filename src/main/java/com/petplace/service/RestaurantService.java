package com.petplace.service;

import com.petplace.dto.request.RestaurantFilterRequest;
import com.petplace.dto.request.RestaurantRequest;
import com.petplace.dto.request.RestaurantUpdateRequest;
import com.petplace.dto.response.RestaurantResponse;
import com.petplace.dto.response.OwnerRestaurantSummaryResponse; // 💡 [추가] 컨트롤러에서 사용하는 Response DTO 임포트
import com.petplace.entity.Restaurant;
import com.petplace.entity.RestaurantImage;
import com.petplace.entity.User;
import com.petplace.exception.BusinessException;
import com.petplace.exception.ErrorCode;
import com.petplace.repository.BookmarkRepository;
import com.petplace.repository.RestaurantRepository;
import com.petplace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, rollbackFor = Exception.class)
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;
    private final BookmarkRepository bookmarkRepository;
    private final FileService fileService;

    /**
     * 사장님 승인 여부 검증 헬퍼 메서드
     */
    private void validateOwnerVerified(User owner) {
        if (!owner.isVerified()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_OWNER);
        }
    }

    /**
     * 💡 [추가된 메서드] 사장님이 등록한 장소(가게) 목록 조회
     * OwnerRestaurantController의 에러를 해결하기 위한 메서드입니다.
     */
    public List<OwnerRestaurantSummaryResponse> getMyRestaurants(Long ownerId) {
        // 레포지토리 메서드명이 프로젝트 규칙에 따라 findByOwner_Id 혹은 findAllByOwnerId 일 수 있으므로 확인 필요
        List<Restaurant> restaurants = restaurantRepository.findAllByOwnerId(ownerId);

        return restaurants.stream()
                .map(OwnerRestaurantSummaryResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 내 주변 장소 조회
     */
    public Page<RestaurantResponse> findNearby(double lat, double lng, double radius, Pageable pageable) {
        return restaurantRepository.findNearby(lat, lng, radius, pageable)
                .map(RestaurantResponse::from);
    }

    /**
     * 가게 상세 정보 조회
     */
    public RestaurantResponse getRestaurantDetail(Long id, Long userId) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESTAURANT_NOT_FOUND));

        RestaurantResponse response = RestaurantResponse.from(restaurant);

        if (userId != null) {
            boolean isBookmarked = bookmarkRepository.existsByUserIdAndRestaurantId(userId, id);
            response.setBookmarked(isBookmarked);
        } else {
            response.setBookmarked(false);
        }

        return response;
    }

    /**
     * 조건 필터 검색 및 북마크 매핑
     */
    public Page<RestaurantResponse> searchRestaurants(Long userId, RestaurantFilterRequest condition, Pageable pageable) {
        Page<Restaurant> restaurantPage = restaurantRepository.findByFilters(condition, pageable);

        Set<Long> bookmarkedRestaurantIds = Collections.emptySet();
        if (userId != null && !restaurantPage.isEmpty()) {
            List<Long> restaurantIds = restaurantPage.getContent().stream()
                    .map(Restaurant::getId)
                    .collect(Collectors.toList());

            bookmarkedRestaurantIds = bookmarkRepository.findRestaurantIdsByUserIdAndRestaurantIdIn(userId, restaurantIds);
        }

        final Set<Long> finalBookmarkedIds = bookmarkedRestaurantIds;
        return restaurantPage.map(restaurant -> {
            RestaurantResponse response = RestaurantResponse.from(restaurant);
            response.setBookmarked(finalBookmarkedIds.contains(restaurant.getId()));
            return response;
        });
    }

    /**
     * 신규 장소 등록
     */
    @Transactional(rollbackFor = Exception.class)
    public Long register(Long ownerId, RestaurantRequest req, List<MultipartFile> images) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (owner.getRole() != User.Role.OWNER) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

        // 💡 신규 등록 시 사장님 승인 여부 검증 해제 완료
        // validateOwnerVerified(owner);

        if (restaurantRepository.existsByBusinessNo(req.getBusinessNo())) {
            throw new BusinessException(ErrorCode.DUPLICATE_BUSINESS_NUMBER);
        }

        Restaurant restaurant = req.toEntity();
        restaurant.assignOwner(owner);

        List<String> uploadedFiles = new ArrayList<>();

        if (images != null && !images.isEmpty()) {
            int order = 0;
            for (MultipartFile file : images) {
                if (file != null && !file.isEmpty()) {
                    String imageUrl = fileService.uploadFile(file);
                    if (imageUrl != null) {
                        uploadedFiles.add(imageUrl);
                        restaurant.addImage(new RestaurantImage(imageUrl, restaurant, order++));
                    }
                }
            }
        }

        restaurantRepository.save(restaurant);

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                if (status == STATUS_ROLLED_BACK) {
                    uploadedFiles.forEach(fileService::deleteFile);
                }
            }
        });

        return restaurant.getId();
    }

    /**
     * 장소 정보 및 이미지 수정
     */
    @Transactional(rollbackFor = Exception.class)
    public Long update(Long id, Long ownerId, RestaurantUpdateRequest req, List<MultipartFile> newImages) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESTAURANT_NOT_FOUND));

        if (restaurant.getOwner() == null || !Objects.equals(restaurant.getOwner().getId(), ownerId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

        validateOwnerVerified(restaurant.getOwner());

        // 💡 사업자등록번호 수정 차단 및 기존 중복 체크 로직 제거 완료
        restaurant.update(
                req.getName(), req.getAddress(), req.getPhone(),
                req.toOperatingHourEntities(),
                req.isHasIndoor(), req.isHasOutdoor(), req.isHasRestroom(),
                req.isAllowSmall(), req.isAllowMedium(), req.isAllowLarge()
        );

        final List<String> urlsToDelete = new ArrayList<>();
        final List<String> newlyUploadedFiles = new ArrayList<>();

        // 이미지 편집 로직: 살릴 이미지와 지울 이미지 발라내기
        List<RestaurantImage> currentImages = restaurant.getImages();
        List<RestaurantImage> imagesToKeep = new ArrayList<>();

        for (RestaurantImage img : currentImages) {
            // existingImageUrls == null: 클라이언트가 필드 미전송 → 기존 이미지 전체 유지
            // existingImageUrls == []: 모두 삭제
            // existingImageUrls == [...]: 목록에 있는 것만 유지
            if (req.getExistingImageUrls() == null || req.getExistingImageUrls().contains(img.getImageUrl())) {
                imagesToKeep.add(img);
            } else {
                urlsToDelete.add(img.getImageUrl());
            }
        }

        restaurant.updateImages(imagesToKeep);

        if (newImages != null && !newImages.isEmpty()) {
            int currentOrder = imagesToKeep.size();
            uploadFilesAndCreateEntities(restaurant, newImages, newlyUploadedFiles, currentOrder);
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                for (String imageUrl : urlsToDelete) {
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

        return restaurant.getId();
    }

    /**
     * 파일 업로드 및 연관관계 동기화 헬퍼
     */
    private void uploadFilesAndCreateEntities(Restaurant restaurant, List<MultipartFile> images, List<String> newlyUploadedFiles, int startOrder) {
        if (images == null || images.isEmpty()) return;

        int order = startOrder;
        for (MultipartFile file : images) {
            if (file != null && !file.isEmpty()) {
                String imageUrl = fileService.uploadFile(file);

                if (imageUrl != null) {
                    newlyUploadedFiles.add(imageUrl);
                    RestaurantImage imgEntity = new RestaurantImage(imageUrl, restaurant, order++);
                    restaurant.addImage(imgEntity);
                }
            }
        }
    }
}