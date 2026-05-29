package com.petplace.service;

import com.petplace.dto.request.RestaurantFilterRequest;
import com.petplace.dto.request.RestaurantRequest;
import com.petplace.dto.request.RestaurantUpdateRequest; // 💡 변경된 수정 전용 DTO 임포트
import com.petplace.dto.response.RestaurantResponse;
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

        // 💡 [요청 반영]: 신규 등록 시 사장님 승인 여부 검증 제한 해제
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

        // 💡 [요청 반영]: 사업자등록번호 수정 불가 처리에 따라 기존 중복 체크 로직 제거

        // 💡 [안전 장치]: 기존 엔티티의 사업자등록번호(restaurant.getBusinessNo())를 그대로 다시 넘겨줌으로써
        // Restaurant 엔티티 클래스의 대대적인 수정 없이도 사업자번호 수정을 원천 차단합니다.
        restaurant.update(
                req.getName(), req.getAddress(), req.getPhone(),
                req.toOperatingHourEntities(),
                req.isHasIndoor(), req.isHasOutdoor(), req.isHasRestroom(),
                req.isAllowSmall(), req.isAllowMedium(), req.isAllowLarge()
        );

        final List<String> urlsToDelete = new ArrayList<>();      // S3에서 지워야 할 기존 파일들
        final List<String> newlyUploadedFiles = new ArrayList<>(); // 실패 시 롤백용 새 파일들

        // 이미지 편집 로직: 살릴 이미지와 지울 이미지 발라내기
        List<RestaurantImage> currentImages = restaurant.getImages();
        List<RestaurantImage> imagesToKeep = new ArrayList<>();

        for (RestaurantImage img : currentImages) {
            // 프론트엔드가 유지하겠다고 보낸 목록에 포함되어 있다면 유지
            if (req.getExistingImageUrls() != null && req.getExistingImageUrls().contains(img.getImageUrl())) {
                imagesToKeep.add(img);
            } else {
                // 목록에 없으면 삭제 대상 분류
                urlsToDelete.add(img.getImageUrl());
            }
        }

        // 엔티티 내 이미지 목록을 1차적으로 '유지할 이미지'로 세팅
        restaurant.updateImages(imagesToKeep);

        // 💡 [해결 포인트]: 4개의 파라미터(시작 순서 포함)를 전달하여 순서 매핑 오류 방지
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
     * 💡 [해결 포인트]: 파라미터 맨 뒤에 'int startOrder'를 추가하여 호출부와 시그니처를 완전히 일치시켰습니다.
     */
    private void uploadFilesAndCreateEntities(Restaurant restaurant, List<MultipartFile> images, List<String> newlyUploadedFiles, int startOrder) {
        if (images == null || images.isEmpty()) return;

        int order = startOrder; // 0번 고정이 아니라 유지된 기존 사진 개수 다음 번호부터 부여
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