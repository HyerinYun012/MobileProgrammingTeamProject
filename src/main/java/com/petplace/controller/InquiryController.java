package com.petplace.controller;

import com.petplace.dto.request.InquiryRequest;
import com.petplace.entity.*;
import com.petplace.repository.InquiryRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Tag(name = "고객 문의(Inquiry) API", description = "1:1 문의하기 및 고객 지원 관련 API")
@RestController
@RequestMapping("/api/inquiries")
@RequiredArgsConstructor
public class InquiryController {
    private final InquiryRepository inquiryRepo;

    @Operation(
            summary = "1:1 문의 제출",
            description = "사용자 ID와 문의 내용을 받아 새로운 문의 사항을 접수합니다."
    )
    @PostMapping
    public ResponseEntity<?> submit(
            @Parameter(description = "문의를 작성하는 사용자의 고유 ID", example = "1")
            @RequestParam Long userId,
            @RequestBody InquiryRequest req
    ) {
        Inquiry i = new Inquiry();
        i.setUser(new User(userId));
        i.setCategory(req.getCategory());
        i.setEmail(req.getEmail());
        i.setContent(req.getContent());
        i.setImageUrl(req.getImageUrl());

        inquiryRepo.save(i);
        return ResponseEntity.ok(Map.of("message", "문의 접수 완료"));
    }
}