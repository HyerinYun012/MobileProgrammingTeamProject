package com.petplace.service;

import com.petplace.dto.request.RestaurantFilterRequest;
import com.petplace.dto.request.RestaurantRequest;
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
     * 내 주변 장소 조회
     */
    public Page<RestaurantResponse> findNearby(double lat, double lng, double radius, Pageable pageable) {
        return restaurantRepository.findNearby(lat, lng, radius, pageable)
                .map(RestaurantResponse::from);
    }

    /**
     * 가게 상세 정보 조회 (북마크 여부 결합 및 비로그인 대응)
     */
    public RestaurantResponse getRestaurantDetail(Long id, Long userId) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESTAURANT_NOT_FOUND));

        RestaurantResponse response = RestaurantResponse.from(restaurant);

        // 사용자가 로그인 상태라면 테이블을 조회하여 북마크 Flag 설정
        if (userId != null) {
            boolean isBookmarked = bookmarkRepository.existsByUserIdAndRestaurantId(userId, id);
            response.setBookmarked(isBookmarked); // 💡 setIsBookmarked -> setBookmarked로 변경
        } else {
            response.setBookmarked(false); // 💡 setIsBookmarked -> setBookmarked로 변경
        }

        return response;
    }

    /**
     * 조건 필터 검색 및 북마크 매핑 (비로그인 유저 대응 완료)
     */
    public Page<RestaurantResponse> searchRestaurants(Long userId, RestaurantFilterRequest condition, Pageable pageable) {
        // 1. 리포지토리로부터 엔티티 페이징 데이터 획득
        Page<Restaurant> restaurantPage = restaurantRepository.findByFilters(condition, pageable);

        // 2. 로그인 유저인 경우 현재 페이지의 장소 IDs 기반으로 북마크 목록을 단 1회 대량 조회(성능 최적화)
        Set<Long> bookmarkedRestaurantIds = Collections.emptySet();
        if (userId != null && !restaurantPage.isEmpty()) {
            List<Long> restaurantIds = restaurantPage.getContent().stream()
                    .map(Restaurant::getId)
                    .collect(Collectors.toList());

            // 유저가 북마크한 장소 ID 셋 추출
            bookmarkedRestaurantIds = bookmarkRepository.findRestaurantIdsByUserIdAndRestaurantIdIn(userId, restaurantIds);
        }

        // 3. 엔티티 데이터 루프돌며 DTO 매핑 진행 시 북마크 포함 여부 판단 가공
        final Set<Long> finalBookmarkedIds = bookmarkedRestaurantIds;
        return restaurantPage.map(restaurant -> {
            // 💡 인수가 1개인 기존 from 메서드를 호출한 뒤, Setter로 북마크 Flag를 주입하도록 수정
            RestaurantResponse response = RestaurantResponse.from(restaurant);
            response.setBookmarked(finalBookmarkedIds.contains(restaurant.getId()));
            return response;
        });
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