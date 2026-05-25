package com.petplace.entity;
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
    private boolean isVerified=false, allowSmall=false, allowMedium=false, allowLarge=false;
    private boolean hasFence=false, hasArtificialGrass=false, hasNaturalGrass=false, hasSnack=false;
    private boolean hasParking=false, hasRestroom=false, hasIndoor=false, hasOutdoor=false;
    @OneToMany(mappedBy = "restaurant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<RestaurantImage> images = new ArrayList<>();
    @OneToMany(mappedBy = "restaurant", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Review> reviews = new ArrayList<>();
    @CreationTimestamp private LocalDateTime createdAt;
    @UpdateTimestamp private LocalDateTime updatedAt;
    public Restaurant(Long id) { this.id = id; }
    public enum Category { 일반음식점, 휴게음식점, 제과점 }
    public enum Region { 대야동,신천동,신현동,은행동,매화동,목감동,군자동,월곶동,정왕동,거북섬동,배곤동,과림동,연성동,능곡동,장곡동 }
}
