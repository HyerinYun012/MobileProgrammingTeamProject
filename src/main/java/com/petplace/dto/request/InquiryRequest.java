package com.petplace.dto.request;
import com.petplace.entity.Inquiry;
import lombok.Data;
@Data public class InquiryRequest { private Inquiry.Category category; private String email, content, imageUrl; }
