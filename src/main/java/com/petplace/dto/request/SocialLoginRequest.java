package com.petplace.dto.request;
import lombok.Data;
@Data public class SocialLoginRequest {
    private String provider, providerId, nickname, phone, role;
    private boolean marketingAgree;
}
