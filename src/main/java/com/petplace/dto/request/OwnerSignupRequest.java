package com.petplace.dto.request;
import lombok.Data;
@Data public class OwnerSignupRequest {
    private String loginId, password, passwordConfirm, nickname, phone, businessNo, businessAddress;
    private boolean marketingAgree;
}
