package com.petplace.dto.response;

import com.petplace.entity.Menu;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class MenuResponse {
    private Long id;
    private String name;
    private Integer price;
    private String description;
    private String imageUrl;

    public static MenuResponse from(Menu menu) {
        return MenuResponse.builder()
                .id(menu.getId())
                .name(menu.getName())
                .price(menu.getPrice())
                .description(menu.getDescription())
                .imageUrl(menu.getImageUrl())
                .build();
    }
}