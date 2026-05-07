package com.petplace.dto.request;
import lombok.Data;
@Data public class CustomerSignupRequest {
    private String name, loginId, password, passwordConfirm, nickname, phone;
}
