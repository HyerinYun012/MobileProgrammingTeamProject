package com.petplace.service;

import com.petplace.dto.request.RestaurantFilterRequest;
import com.petplace.dto.request.RestaurantRequest;
import com.petplace.entity.Restaurant;
import com.petplace.entity.RestaurantImage;
import com.petplace.entity.User;
import com.petplace.exception.BusinessException;
import com.petplace.repository.RestaurantRepository;
import com.petplace.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

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
    public List<Restaurant> findNearby(double lat, double lng, double radius) {
        return restaurantRepository.findNearby(lat, lng, radius);
    }

    /**
     * 조건 필터링 검색 (Querydsl)
     */
    public List<Restaurant> searchRestaurants(RestaurantFilterRequest condition) {
        return restaurantRepository.findByFilters(condition);
    }

    /**
     * 상세 정보 조회
     */
    public Restaurant getDetail(Long id) {
        return restaurantRepository.findById(id)
                .orElseThrow(() -> new BusinessException("해당 장소 정보를 찾을 수 없습니다."));
    }

    /**
     * 신규 장소 등록
     */
    @Transactional(rollbackFor = Exception.class)
    public Long register(Long ownerId, RestaurantRequest req, List<MultipartFile> images) {
        User owner = userRepository.findById(ownerId)
                .orElseThrow(() -> new BusinessException("존재하지 않는 사용자입니다."));

        if (owner.getRole() != User.Role.OWNER) {
            throw new BusinessException("장소 등록 권한이 없는 계정입니다.");
        }

        if (restaurantRepository.existsByBusinessNo(req.getBusinessNo())) {
            throw new BusinessException("이미 등록된 사업자 번호입니다.");
        }

        // 🌟 [교정] 영업시간(operatingHours) 선택 입력 방어선이 완비된 도메인 생성자 체계 작동
        Restaurant restaurant = req.toEntity();
        restaurant.assignOwner(owner);

        // 이미지 파일 업로드 및 객체 연관관계 동기화
        List<RestaurantImage> restaurantImages = uploadFilesAndCreateEntities(restaurant, images);

        // 🌟 [버그 해결] 엔티티 내부 컬렉션 리스트에 이미지들을 명시적으로 주입 (Cascade 영속성 전이 발동)
        if (!restaurantImages.isEmpty()) {
            restaurant.getImages().addAll(restaurantImages);
        }

        return restaurantRepository.save(restaurant).getId();
    }

    /**
     * 장소 정보 및 이미지 수정
     */
    @Transactional(rollbackFor = Exception.class)
    public Long update(Long id, Long ownerId, RestaurantRequest req, List<MultipartFile> newImages) {
        Restaurant restaurant = restaurantRepository.findById(id)
                .orElseThrow(() -> new BusinessException("해당 장소 정보를 찾을 수 없습니다."));

        if (restaurant.getOwner() == null || !Objects.equals(restaurant.getOwner().getId(), ownerId)) {
            throw new BusinessException("해당 장소의 수정 권한이 없습니다.");
        }

        // 1. 가게 기본 정보 변경 (Dirty Checking - 엔티티 내 선택 입력 방어선 가동)
        restaurant.update(req);

        // 2. 새로운 이미지 리스트 교체 요청이 있는 경우
        if (newImages != null && !newImages.isEmpty()) {
            // S3 원격 저장소의 물리 파일 선제거 수행
            List<RestaurantImage> oldImages = new ArrayList<>(restaurant.getImages());
            for (RestaurantImage oldImg : oldImages) {
                fileService.deleteFile(oldImg.getImageUrl());
            }

            // 신규 파일 업로드 가동 및 자식 엔티티 빌드
            List<RestaurantImage> uploadedImages = uploadFilesAndCreateEntities(restaurant, newImages);

            // 🌟 [경고 완전 소멸] Restaurant 엔티티에 잠들어있던 updateImages() 도메인 메서드 연결!
            // 내부 컬렉션을 스스로 clear하고 addAll하게 함으로써 객체지향적 캡슐화 완성 및 Unused 경고 해결
            restaurant.updateImages(uploadedImages);
        }

        return restaurant.getId();
    }

    /**
     * 파일 스토리지 업로드 및 자식 엔티티 리스트 생성 분리 (비즈니스 헬퍼 메서드)
     * 💡 체크드 예외(IOException)를 런타임 예외로 안전하게 감싸서 메서드 시그니처를 정돈합니다.
     */
    private List<RestaurantImage> uploadFilesAndCreateEntities(Restaurant restaurant, List<MultipartFile> images) {
        List<RestaurantImage> imageEntities = new ArrayList<>();

        if (images != null && !images.isEmpty()) {
            int order = 0;
            try {
                for (MultipartFile file : images) {
                    if (file != null && !file.isEmpty()) {
                        // S3 물리 저장소 업로드 후 경로 추출
                        String imageUrl = fileService.uploadFile(file);

                        // 자식 이미지 인스턴스 조립
                        RestaurantImage imgEntity = new RestaurantImage(imageUrl, restaurant, order++);
                        imageEntities.add(imgEntity);
                    }
                }
            } catch (IOException e) {
                log.error("가게 이미지 파일 업로드 중 입출력 예외 발생", e);
                throw new BusinessException("이미지 파일 저장 시스템에 오류가 발생했습니다. 다시 시도해 주세요.");
            }
        }
        return imageEntities;
    }
}