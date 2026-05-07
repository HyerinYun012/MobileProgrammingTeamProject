package com.petplace.controller;
import com.petplace.dto.request.InquiryRequest;
import com.petplace.entity.*;
import com.petplace.repository.InquiryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController @RequestMapping("/api/inquiries") @RequiredArgsConstructor
public class InquiryController {
    private final InquiryRepository inquiryRepo;

    @PostMapping
    public ResponseEntity<?> submit(@RequestParam Long userId, @RequestBody InquiryRequest req) {
        Inquiry i = new Inquiry(); i.setUser(new User(userId)); i.setCategory(req.getCategory());
        i.setEmail(req.getEmail()); i.setContent(req.getContent()); i.setImageUrl(req.getImageUrl());
        inquiryRepo.save(i);
        return ResponseEntity.ok(Map.of("message","문의 접수 완료"));
    }
}
