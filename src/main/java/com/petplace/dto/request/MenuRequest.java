package com.petplace.dto.request;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class MenuRequest {
    private String name;
    private int price;
    private String description;
}