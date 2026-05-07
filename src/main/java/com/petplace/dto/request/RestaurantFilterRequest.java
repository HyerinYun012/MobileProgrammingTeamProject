package com.petplace.dto.request;
import lombok.Data;
@Data public class RestaurantFilterRequest {
    private String region;
    private Boolean hasParking, hasRestroom, allowSmall, allowMedium, allowLarge;
    private Boolean hasFence, hasArtificialGrass, hasNaturalGrass, hasSnack, hasIndoor, hasOutdoor;
}
