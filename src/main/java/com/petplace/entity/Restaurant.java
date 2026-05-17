package com.petplace.entity;

import com.petplace.dto.request.RestaurantRequest;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "restaurants")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Restaurant extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id", nullable = false)
    private User owner;

    @Column(nullable = false, length = 100)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Category category;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Region region;

    @Column(length = 200)
    private String address;

    @Column(precision = 10, scale = 7)
    private BigDecimal latitude;

    @Column(precision = 10, scale = 7)
    private BigDecimal longitude;

    @Column(length = 20)
    private String phone;

    @Column(length = 20)
    private String businessNo;

    @Column(length = 200)
    private String operatingHours;

    @SuppressWarnings("FieldMayBeFinal")
    @Column(nullable = false)
    private boolean isVerified = false;

    private boolean allowSmall = false;
    private boolean allowMedium = false;
    private boolean allowLarge = false;
    private boolean hasFence = false;
    private boolean hasArtificialGrass = false;
    private boolean hasNaturalGrass = false;
    private boolean hasSnack = false;
    private boolean hasParking = false;
    private boolean hasRestroom = false;
    private boolean hasIndoor = false;
    private boolean hasOutdoor = false;

    @OneToMany(mappedBy = "restaurant", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private final List<RestaurantImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "restaurant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private final List<Review> reviews = new ArrayList<>();

    @OneToMany(mappedBy = "restaurant", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private final List<Menu> menus = new ArrayList<>();

    // Test 및 인프라용 생성자
    public Restaurant(Long id) {
        this.id = id;
    }

    /**
     * RestaurantRequest의 toEntity()용 도메인 비즈니스 생성자
     */
    public Restaurant(String name, String address, String phone, String businessNo,
                      Category category, Region region, BigDecimal latitude, BigDecimal longitude,
                      String operatingHours, boolean hasFence, boolean hasArtificialGrass,
                      boolean hasNaturalGrass, boolean hasSnack, boolean hasParking,
                      boolean hasRestroom, boolean hasIndoor, boolean hasOutdoor,
                      boolean allowSmall, boolean allowMedium, boolean allowLarge) {
        this.name = name;
        this.address = address;
        this.phone = phone;
        this.businessNo = businessNo;
        this.category = category;
        this.region = region;
        this.latitude = latitude;
        this.longitude = longitude;

        // 🌟 영업시간 선택 입력 방어: null이거나 공백 문자가 유입될 시 빈 스트링("") 처리
        this.operatingHours = (operatingHours == null || operatingHours.isBlank()) ? "" : operatingHours;

        this.hasFence = hasFence;
        this.hasArtificialGrass = hasArtificialGrass;
        this.hasNaturalGrass = hasNaturalGrass;
        this.hasSnack = hasSnack;
        this.hasParking = hasParking;
        this.hasRestroom = hasRestroom;
        this.hasIndoor = hasIndoor;
        this.hasOutdoor = hasOutdoor;
        this.allowSmall = allowSmall;
        this.allowMedium = allowMedium;
        this.allowLarge = allowLarge;
    }

    public void assignOwner(User owner) {
        this.owner = owner;
    }

    /**
     * 정보 수정을 위한 비즈니스 메서드 (Dirty Checking)
     */
    public void update(RestaurantRequest req) {
        this.name = req.getName();
        this.category = req.getCategory();
        this.region = req.getRegion();
        this.address = req.getAddress();
        this.phone = req.getPhone();
        this.businessNo = req.getBusinessNo();
        this.latitude = req.getLatitude();
        this.longitude = req.getLongitude();

        // 🌟 수정 요청 시에도 영업시간 데이터 유실 방어선 작동
        this.operatingHours = (req.getOperatingHours() == null || req.getOperatingHours().isBlank()) ? "" : req.getOperatingHours();

        this.hasFence = req.isHasFence();
        this.hasArtificialGrass = req.isHasArtificialGrass();
        this.hasNaturalGrass = req.isHasNaturalGrass();
        this.hasSnack = req.isHasSnack();
        this.hasParking = req.isHasParking();
        this.hasRestroom = req.isHasRestroom();
        this.hasIndoor = req.isHasIndoor();
        this.hasOutdoor = req.isHasOutdoor();
        this.allowSmall = req.isAllowSmall();
        this.allowMedium = req.isAllowMedium();
        this.allowLarge = req.isAllowLarge();
    }

    /**
     * 🌟 [컴파일 에러 해결 지점] 이미지 컬렉션을 완전히 교체하기 위한 도메인 메서드
     * JPA의 orphanRemoval 체계가 안전하게 동작하도록 내부 요소를 청소(clear)한 뒤 주입합니다.
     */
    public void updateImages(List<RestaurantImage> newImages) {
        this.images.clear();
        if (newImages != null) {
            this.images.addAll(newImages);
        }
    }

    public String getImageUrl() {
        if (this.images.isEmpty()) {
            return null;
        }
        RestaurantImage firstImage = this.images.get(0);
        return firstImage != null ? firstImage.getImageUrl() : null;
    }

    @RequiredArgsConstructor
    @Getter
    public enum Category {
        RESTAURANT("일반음식점"),
        REST_AREA("휴게음식점"),
        BAKERY("제과점"),
        CAFE("카페");

        private final String description;
    }

    @RequiredArgsConstructor
    @Getter
    public enum Region {
        DAEYA("대야동"), SINCHEON("신천동"), SINHYEON("신현동"), EUNHAENG("은행동"),
        MAEHWA("매화동"), MOKGAM("목감동"), GUNJA("군자동"), WOLGOT("월곶동"),
        JEONGWANG("정왕동"), GEOBUKSEOM("거북섬동"), BAEGON("배곧동"), GWARIM("과림동"),
        YEONSEONG("연성동"), NEUNGGOK("능곡동"), JANGGOK("장곡동");

        private final String krName;
    }
}