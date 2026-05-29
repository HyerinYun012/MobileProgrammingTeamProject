package com.petplace.service;

import com.petplace.dto.request.RestaurantFilterRequest;
import com.petplace.dto.request.RestaurantRequest;
import com.petplace.dto.request.RestaurantUpdateRequest;
import com.petplace.dto.response.OwnerRestaurantSummaryResponse;
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
     * 💡 [추가] 사장님 승인 여부 검증 헬퍼 메서드
     */
    private void validateOwnerVerified(User owner) {
        if (!owner.isVerified()) { // User 엔티티의 승인 여부 필드 (명칭에 맞게 수정)
            throw new BusinessException(ErrorCode.UNAUTHORIZED_OWNER); // 승인되지 않은 사장님 예외
        }
    }

    /**
     * 사장님 본인 업장 목록 조회
     */
    public List<OwnerRestaurantSummaryResponse> getMyRestaurants(Long ownerId) {
        return restaurantRepository.findAllByOwnerId(ownerId).stream()
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
     * 가게 상세 정보 조회 (북마크 여부 결합 및 비로그인 대응)
     */
    public RestaurantResponse getRestaurantDetail(Long id, Long userId) {
        Restaurant restaurant = restaurantRepository.findByIdWithImages(id)
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
     * 조건 필터 검색 및 북마크 매핑 (비로그인 유저 대응 완료)
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
     * ★ 규칙 4 적용: null 반환 예외 방어 가드 장치 및 롤백 S3 청소 공정 고도화
     */
    @Transactional(rollbackFor = Exception.class)
    public Long register(Long ownerId, RestaurantRequest req, List<MultipartFile> images) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (owner.getRole() != User.Role.OWNER) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

        // validateOwnerVerified(owner);

        if (restaurantRepository.existsByBusinessNo(req.getBusinessNo())) {
            throw new BusinessException(ErrorCode.DUPLICATE_BUSINESS_NUMBER);
        }

        Restaurant restaurant = req.toEntity();
        restaurant.assignOwner(owner);

        List<String> uploadedFiles = new ArrayList<>(); // 롤백 대비 새 파일 추적용 리스트

        if (images != null && !images.isEmpty()) {
            int order = 0;
            for (MultipartFile file : images) {
                // 💡 규칙 4: 비어있는 파일 유입 방어 코드 추가
                if (file != null && !file.isEmpty()) {
                    String imageUrl = fileService.uploadFile(file);

                    // 🌟 [핵심 null 반환 예외 방어]: S3 업로드가 완벽히 성공해서 URL 주소가 리턴되었을 때만 주입 및 매핑 진행
                    if (imageUrl != null) {
                        uploadedFiles.add(imageUrl);
                        restaurant.addImage(new RestaurantImage(imageUrl, restaurant, order++));
                    }
                }
            }
        }

        // DB 저장 프로세스 실행
        restaurantRepository.save(restaurant);

        // ★ 규칙 4: 트랜잭션 롤백-S3 동기화 대책
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCompletion(int status) {
                // 저장 중 예외가 나서 트랜잭션이 최종 롤백되었다면, 업로드된 S3 새 파일 추적 제거
                if (status == STATUS_ROLLED_BACK) {
                    uploadedFiles.forEach(fileService::deleteFile);
                }
            }
        });

        return restaurant.getId();
    }

    @Transactional(rollbackFor = Exception.class)
    public Long update(Long id, Long ownerId, RestaurantUpdateRequest req, List<MultipartFile> newImages) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESTAURANT_NOT_FOUND));

        if (restaurant.getOwner() == null || !Objects.equals(restaurant.getOwner().getId(), ownerId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

        validateOwnerVerified(restaurant.getOwner());

        // 2. 엔티티 기본 정보 및 영업시간 갱신
        restaurant.update(
                req.getName(), req.getAddress(), req.getPhone(), req.toOperatingHourEntities(),
                req.isHasIndoor(), req.isHasOutdoor(), req.isHasRestroom(),
                req.isAllowSmall(), req.isAllowMedium(), req.isAllowLarge()
        );

        final List<String> oldImageUrls = new ArrayList<>();
        final List<String> newlyUploadedFiles = new ArrayList<>();

        // 3. [핵심 수정] 이미지 전체 교체 로직 흐름 정밀화
        if (newImages != null && !newImages.isEmpty()) {
            // ⓐ 지워질 기존 S3 파일 주소 백업
            oldImageUrls.addAll(restaurant.getImages().stream()
                    .map(RestaurantImage::getImageUrl)
                    .collect(Collectors.toList()));

            // ⓑ 기존 자식 엔티티 리스트를 깨끗하게 비워서 고립 (orphanRemoval=true 가 작동하여 DB Delete 유도)
            restaurant.getImages().clear();

            // ⓒ 임시 리스트를 만들어 S3 새 업로드 및 새 엔티티 생성 진행
            List<RestaurantImage> freshImages = new ArrayList<>();
            int order = 0;
            for (MultipartFile file : newImages) {
                if (file != null && !file.isEmpty()) {
                    String imageUrl = fileService.uploadFile(file);
                    if (imageUrl != null) {
                        newlyUploadedFiles.add(imageUrl);
                        // 순수 객체 생성 (아직 레스토랑 내부 리스트에 add 하지 않음)
                        freshImages.add(new RestaurantImage(imageUrl, restaurant, order++));
                    }
                }
            }

            // ⓓ 완벽히 필터링된 새 이미지 엔티티 세트만 온전히 주입
            restaurant.updateImages(freshImages);
        }

        // 4. 트랜잭션 동기화 (기존 동일)
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

        return restaurant.getId();
    }
}