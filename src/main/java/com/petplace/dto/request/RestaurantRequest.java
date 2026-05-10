package com.petplace.dto.request;

import com.petplace.entity.Restaurant;
import com.petplace.entity.User; // User 엔티티 임포트
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Schema(description = "장소 등록 및 수정 요청 객체 (사장님용)")
public class RestaurantRequest {

    // ... (기존 필드 선언부와 동일) ...
    @NotBlank private String name;
    @NotBlank private String address;
    @NotBlank private String phone;
    @NotBlank private String businessNo;
    @NotNull private Restaurant.Category category;
    @NotNull private Restaurant.Region region;
    @NotNull private BigDecimal latitude;
    @NotNull private BigDecimal longitude;
    private boolean hasFence;
    private boolean hasArtificialGrass;
    private boolean hasNaturalGrass;
    private boolean hasSnack;
    private boolean hasParking;
    private boolean hasRestroom;
    private boolean hasIndoor;
    private boolean hasOutdoor;
    private boolean allowSmall;
    private boolean allowMedium;
    private boolean allowLarge;

    /**
     * [추가] DTO를 엔티티로 변환하는 메서드
     * @param ownerId 인증된 사장님(User)의 ID
     */
    public Restaurant toEntity(Long ownerId) {
        Restaurant restaurant = new Restaurant();

        // 연관관계 설정을 위해 ID만 가진 User 객체 생성 (Proxy 성격)
        User owner = new User();
        owner.setId(ownerId);
        restaurant.setOwner(owner);

        restaurant.setName(this.name);
        restaurant.setAddress(this.address);
        restaurant.setPhone(this.phone);
        restaurant.setBusinessNo(this.businessNo);
        restaurant.setCategory(this.category);
        restaurant.setRegion(this.region);
        restaurant.setLatitude(this.latitude);
        restaurant.setLongitude(this.longitude);

        // 시설 정보 설정
        restaurant.setHasFence(this.hasFence);
        restaurant.setHasArtificialGrass(this.hasArtificialGrass);
        restaurant.setHasNaturalGrass(this.hasNaturalGrass);
        restaurant.setHasSnack(this.hasSnack);
        restaurant.setHasParking(this.hasParking);
        restaurant.setHasRestroom(this.hasRestroom);
        restaurant.setHasIndoor(this.hasIndoor);
        restaurant.setHasOutdoor(this.hasOutdoor);
        restaurant.setAllowSmall(this.allowSmall);
        restaurant.setAllowMedium(this.allowMedium);
        restaurant.setAllowLarge(this.allowLarge);

        return restaurant;
    }
}