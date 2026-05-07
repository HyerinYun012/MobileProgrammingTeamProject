package com.petplace.dto.request;
import com.petplace.entity.Restaurant;
import lombok.Data;
import java.math.BigDecimal;
@Data public class RestaurantRequest {
    private String name, address, phone, businessNo;
    private Restaurant.Category category;
    private Restaurant.Region region;
    private BigDecimal latitude, longitude;
    private boolean hasFence, hasArtificialGrass, hasNaturalGrass, hasSnack;
    private boolean hasParking, hasRestroom, hasIndoor, hasOutdoor;
    private boolean allowSmall, allowMedium, allowLarge;
}
