package com.petplace.entity;

import com.petplace.dto.request.RestaurantRequest;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity @Table(name = "restaurants") @Getter @Setter @NoArgsConstructor
public class Restaurant {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "owner_id", nullable = false) private User owner;
    @Column(nullable = false, length = 100) private String name;
    @Enumerated(EnumType.STRING) private Category category;
    @Enumerated(EnumType.STRING) private Region region;
    @Column(length = 200) private String address;
    @Column(precision = 10, scale = 7) private BigDecimal latitude;
    @Column(precision = 10, scale = 7) private BigDecimal longitude;
    @Column(length = 20) private String phone;
    @Column(length = 20) private String businessNo;

    @Column(length = 200) private String operatingHours; // [추가] 영업시간 (예: "매일 10:00 ~ 22:00")

    private boolean isVerified=false, allowSmall=false, allowMedium=false, allowLarge=false;
    private boolean hasFence=false, hasArtificialGrass=false, hasNaturalGrass=false, hasSnack=false;
    private boolean hasParking=false, hasRestroom=false, hasIndoor=false, hasOutdoor=false;

    @OneToMany(mappedBy = "restaurant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RestaurantImage> images = new ArrayList<>();

    @OneToMany(mappedBy = "restaurant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Review> reviews = new ArrayList<>();

    // [추가] 메뉴 연관관계 고도화 (orphanRemoval=true 설정으로 고도화된 변경 감지 지원)
    @OneToMany(mappedBy = "restaurant", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Menu> menus = new ArrayList<>();

    @CreationTimestamp private LocalDateTime createdAt;
    @UpdateTimestamp private LocalDateTime updatedAt;

    public Restaurant(Long id) { this.id = id; }

    // [수정] 정보 수정을 위한 비즈니스 메서드
    public void update(RestaurantRequest req) {
        this.name = req.getName();
        this.category = req.getCategory();
        this.region = req.getRegion();
        this.address = req.getAddress();
        this.phone = req.getPhone();
        this.latitude = req.getLatitude();
        this.longitude = req.getLongitude();
        this.operatingHours = req.getOperatingHours(); // [추가] 영업시간 수정 반영

        // 시설 정보 업데이트
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

    // [추가] 메뉴 리스트 전체를 교체할 때 사용하는 편의 메서드
    public void updateMenus(List<Menu> newMenus) {
        this.menus.clear();
        if (newMenus != null) {
            this.menus.addAll(newMenus);
        }
    }

    public Long getOwnerId() {
        return this.owner != null ? this.owner.getId() : null;
    }

    public enum Category { 일반음식점, 휴게음식점, 제과점 }
    public enum Region { 대야동,신천동,신현동,은행동,매화동,목감동,군자동,월곶동,정왕동,거북섬동,배곤동,과림동,연성동,능곡동,장곡동 }
}