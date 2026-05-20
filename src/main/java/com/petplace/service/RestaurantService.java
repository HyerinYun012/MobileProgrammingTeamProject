package com.petplace.service;

import com.petplace.dto.request.RestaurantFilterRequest;
import com.petplace.dto.request.RestaurantRequest;
import com.petplace.dto.response.RestaurantResponse;
import com.petplace.entity.Restaurant;
import com.petplace.entity.RestaurantImage;
import com.petplace.entity.User;
import com.petplace.exception.BusinessException;
import com.petplace.exception.ErrorCode;
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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true, rollbackFor = Exception.class)
public class RestaurantService {

    private final RestaurantRepository restaurantRepository;
    private final UserRepository userRepository;
    private final FileService fileService;

    /**
     * 내 주변 장소 조회
     */
    public Page<RestaurantResponse> findNearby(double lat, double lng, double radius, Pageable pageable) {
        return restaurantRepository.findNearby(lat, lng, radius, pageable)
                .map(RestaurantResponse::from);
    }

    /**
     * 조건 필터링 검색 (Querydsl)
     */
    public Page<RestaurantResponse> searchRestaurants(RestaurantFilterRequest condition, Pageable pageable) {
        return restaurantRepository.findByFilters(condition, pageable)
                .map(RestaurantResponse::from);
    }

    /**
     * 상세 정보 조회
     */
    public Restaurant getDetail(Long id) {
        return restaurantRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESTAURANT_NOT_FOUND));
    }

    /**
     * 신규 장소 등록
     * 🚀 롤백 시 업로드된 모든 이미지 삭제 로직 및 OperatingHour 구조 매핑 완비
     */
    @Transactional(rollbackFor = Exception.class)
    public Long register(Long ownerId, RestaurantRequest req, List<MultipartFile> images) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (owner.getRole() != User.Role.OWNER) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

        if (restaurantRepository.existsByBusinessNo(req.getBusinessNo())) {
            throw new BusinessException(ErrorCode.DUPLICATE_BUSINESS_NUMBER);
        }

        // 🌟 [수정] RestaurantRequest에서 요일별 영업시간 리스트(List<OperatingHour>)를 엔티티 변환 시 주입하도록 대응
        Restaurant restaurant = req.toEntity();
        restaurant.assignOwner(owner);

        List<String> uploadedFiles = new ArrayList<>(); // 롤백 대비 추적 리스트

        if (images != null && !images.isEmpty()) {
            int order = 0;
            for (MultipartFile file : images) {
                if (file != null && !file.isEmpty()) {
                    String imageUrl = fileService.uploadFile(file);
                    uploadedFiles.add(imageUrl);
                    restaurant.addImage(new RestaurantImage(imageUrl, restaurant, order++));
                }
            }
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

        return restaurantRepository.save(restaurant).getId();
    }

    /**
     * 장소 정보 및 이미지 수정
     * 💡 [정합성 수정] 트랜잭션 커밋 완료 후 S3 파일 삭제 처리 (엑스박스 버그 방지)
     */
    @Transactional(rollbackFor = Exception.class)
    public Long update(Long id, Long ownerId, RestaurantRequest req, List<MultipartFile> newImages) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESTAURANT_NOT_FOUND));

        if (restaurant.getOwner() == null || !Objects.equals(restaurant.getOwner().getId(), ownerId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

        restaurant.update(
                req.getName(), req.getAddress(), req.getPhone(), req.toOperatingHourEntities(),
                req.isHasIndoor(), req.isHasOutdoor(), req.isHasRestroom(),
                req.isAllowSmall(), req.isAllowMedium(), req.isAllowLarge()
        );

        // 2. 새로운 이미지 리스트 교체
        if (newImages != null && !newImages.isEmpty()) {
            // [정합성 개선] 삭제할 이미지 URL 리스트를 먼저 메모리에 확보
            List<String> oldImageUrls = restaurant.getImages().stream()
                    .map(RestaurantImage::getImageUrl)
                    .collect(Collectors.toList());

            // 신규 엔티티 생성 및 연관관계 설정
            uploadFilesAndCreateEntities(restaurant, newImages);

            // 이미지 교체 로직
            restaurant.updateImages(restaurant.getImages());

            // 🚀 트랜잭션 성공 후 커밋된 이후에만 S3 파일 삭제를 진행
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    for (String imageUrl : oldImageUrls) {
                        fileService.deleteFile(imageUrl);
                    }
                }
            });
        }

        return restaurant.getId();
    }

    /**
     * 파일 업로드 및 연관관계 동기화 헬퍼
     */
    private void uploadFilesAndCreateEntities(Restaurant restaurant, List<MultipartFile> images) {
        if (images == null || images.isEmpty()) return;

        int order = 0;
        for (MultipartFile file : images) {
            if (file != null && !file.isEmpty()) {
                String imageUrl = fileService.uploadFile(file);

                RestaurantImage imgEntity = new RestaurantImage(imageUrl, restaurant, order++);
                restaurant.addImage(imgEntity);
            }
        }
    }
}