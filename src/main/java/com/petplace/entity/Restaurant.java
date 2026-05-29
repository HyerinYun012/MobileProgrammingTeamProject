package com.petplace.entity;

import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

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

    // 🌟 [변경] 단일 String 필드 제거 후 값 타입 컬렉션 적용
    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(
            name = "restaurant_operating_hours",
            joinColumns = @JoinColumn(name = "restaurant_id")
    )
    @OrderColumn(name = "day_order") // DB 조회 시 저장된 요일 순서 보장
    private final List<OperatingHour> operatingHours = new ArrayList<>();

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
                      List<OperatingHour> operatingHours, // 🌟 파라미터 변경
                      boolean hasFence, boolean hasArtificialGrass,
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

        // 🌟 [변경] 영업시간 컬렉션 방어적 카피 (null 방지)
        if (operatingHours != null) {
            this.operatingHours.addAll(operatingHours);
        }

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
     * 🌟 파라미터에서 String businessNo를 완전히 제거합니다.
     */
    public void update(String name, String address, String phone,
                       List<OperatingHour> newOperatingHours,
                       boolean hasIndoor, boolean hasOutdoor, boolean hasRestroom,
                       boolean allowSmall, boolean allowMedium, boolean allowLarge) {
        this.name = name;
        this.address = address;
        this.phone = phone;

        this.updateOperatingHours(newOperatingHours);

        this.hasIndoor = hasIndoor;
        this.hasOutdoor = hasOutdoor;
        this.hasRestroom = hasRestroom;
        this.allowSmall = allowSmall;
        this.allowMedium = allowMedium;
        this.allowLarge = allowLarge;
    }

    /**
     * 🌟 [추가] 영업시간 컬렉션 안전 초기화 내부 비즈니스 메서드
     */
    public void updateOperatingHours(List<OperatingHour> newOperatingHours) {
        this.operatingHours.clear(); // 기존 컬렉션 비우기

        if (newOperatingHours != null) {
            // LinkedHashMap을 사용하여 순서를 유지하면서, 요일이 같으면 새 값으로 덮어씁니다.
            // ⚠️ 주의: hour.getDayOfWeek() 부분은 실제 OperatingHour 클래스의 요일 필드/Getter 명칭에 맞추세요.
            Map<Object, OperatingHour> uniqueHoursMap = new java.util.LinkedHashMap<>();

            for (OperatingHour hour : newOperatingHours) {
                if (hour != null) {
                    uniqueHoursMap.put(hour.getDayOfWeek(), hour); // 요일을 Key로 두어 중복 시 자동 덮어쓰기
                }
            }

            // 중복이 완벽히 제거되고 최신화된 영업시간 리스트만 최종 추가
            this.operatingHours.addAll(uniqueHoursMap.values());
        }
    }

    /**
     * [연관관계 편의 메서드] 자식 이미지 등록
     */
    public void addImage(RestaurantImage image) {
        this.images.add(image);
    }

    /**
     * [도메인 메서드] 이미지 컬렉션 교체 (안전한 초기화)
     */
    public void updateImages(List<RestaurantImage> newImages) {
        this.images.clear();
        if (newImages != null) {
            for (RestaurantImage image : newImages) {
                this.addImage(image);
            }
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