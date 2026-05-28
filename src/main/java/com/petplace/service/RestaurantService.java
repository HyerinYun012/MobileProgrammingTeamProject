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

    /**
     * 장소 정보 및 이미지 수정
     * ★ 규칙 4 적용: 커밋(기존 파일 청소) 및 롤백(수정 중 새로 등록된 유령 파일 청소) 양방향 완벽 동기화
     */
    @Transactional(rollbackFor = Exception.class)
    public Long update(Long id, Long ownerId, RestaurantRequest req, List<MultipartFile> newImages) {
        // 1. 기존 데이터 조회 및 권한 체크 (기존과 동일)
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESTAURANT_NOT_FOUND));

        if (restaurant.getOwner() == null || !Objects.equals(restaurant.getOwner().getId(), ownerId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

        if (restaurantRepository.existsByBusinessNoAndIdNot(req.getBusinessNo(), id)) {
            throw new BusinessException(ErrorCode.DUPLICATE_BUSINESS_NUMBER);
        }

        // 2. 엔티티 정보 갱신
        restaurant.update(
                req.getName(), req.getAddress(), req.getPhone(), req.getBusinessNo(),
                req.toOperatingHourEntities(),
                req.isHasIndoor(), req.isHasOutdoor(), req.isHasRestroom(),
                req.isAllowSmall(), req.isAllowMedium(), req.isAllowLarge()
        );

        // 💡 해결 포인트: final로 선언하고 객체 재할당(=)을 하지 않음!
        final List<String> oldImageUrls = new ArrayList<>();
        final List<String> newlyUploadedFiles = new ArrayList<>();

        // 3. 이미지 교체 로직 진행
        if (newImages != null && !newImages.isEmpty()) {
            // 변수 재할당 대신 기존 리스트에 .addAll()로 값만 추가 (Effectively Final 만족)
            oldImageUrls.addAll(restaurant.getImages().stream()
                    .map(RestaurantImage::getImageUrl)
                    .collect(Collectors.toList()));

            // 파일 업로드 헬퍼 호출
            uploadFilesAndCreateEntities(restaurant, newImages, newlyUploadedFiles);
            restaurant.updateImages(restaurant.getImages());
        }

        // 4. 트랜잭션 커밋-롤백 양방향 동기화 대책 (이제 컴파일 에러가 발생하지 않습니다)
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                // 성공 시: 구버전 S3 파일 청소 (리스트가 비어있으면 루프를 돌지 않으므로 안전함)
                for (String imageUrl : oldImageUrls) {
                    fileService.deleteFile(imageUrl);
                }
            }

            @Override
            public void afterCompletion(int status) {
                // 실패 시: 롤백되었다면 이번 요청으로 새롭게 올라간 유령 파일들만 추적 삭제
                if (status == STATUS_ROLLED_BACK) {
                    newlyUploadedFiles.forEach(fileService::deleteFile);
                }
            }
        });

        return restaurant.getId();
    }

    /**
     * 파일 업로드 및 연관관계 동기화 헬퍼 (null 반환 예외 가드 및 새 업로드 추적 연동 확장)
     */
    private void uploadFilesAndCreateEntities(Restaurant restaurant, List<MultipartFile> images, List<String> newlyUploadedFiles) {
        if (images == null || images.isEmpty()) return;

        int order = 0;
        for (MultipartFile file : images) {
            // 💡 규칙 4: 비어있는 파일 유입 차단 방어 코드
            if (file != null && !file.isEmpty()) {
                String imageUrl = fileService.uploadFile(file);

                // 🌟 [핵심 null 반환 예외 방어]: S3가 URL을 확실히 반환했을 때만 연관관계 형성 및 추적 리스트 등록
                if (imageUrl != null) {
                    newlyUploadedFiles.add(imageUrl); // 수정 롤백 대응용 리스트에 보관
                    RestaurantImage imgEntity = new RestaurantImage(imageUrl, restaurant, order++);
                    restaurant.addImage(imgEntity);
                }
            }
        }
    }
}