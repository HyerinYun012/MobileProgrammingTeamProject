package com.petplace.controller;
import com.petplace.dto.request.*;
import com.petplace.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
@RestController @RequestMapping("/api/auth") @RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final SocialAuthService socialAuthService;

    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) { return ResponseEntity.ok(Map.of("token", authService.login(req))); }

    @PostMapping("/signup/customer")
    public ResponseEntity<?> signupCustomer(@RequestBody CustomerSignupRequest req) { authService.signupCustomer(req); return ResponseEntity.ok(Map.of("message","회원가입 완료")); }

    @PostMapping("/signup/owner")
    public ResponseEntity<?> signupOwner(@RequestBody OwnerSignupRequest req) { authService.signupOwner(req); return ResponseEntity.ok(Map.of("message","사장님 가입 완료")); }

    @GetMapping("/check-id")
    public ResponseEntity<?> checkId(@RequestParam String loginId) { return ResponseEntity.ok(Map.of("available", !authService.isLoginIdExists(loginId))); }

    @GetMapping("/check-nickname")
    public ResponseEntity<?> checkNickname(@RequestParam String nickname) { return ResponseEntity.ok(Map.of("available", !authService.isNicknameExists(nickname))); }

    @PostMapping("/find-id")
    public ResponseEntity<?> findId(@RequestBody Map<String,String> req) { return ResponseEntity.ok(Map.of("loginId", authService.findLoginId(req.get("name"), req.get("phone")))); }

    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String,String> req) { authService.resetPassword(req.get("loginId"), req.get("phone"), req.get("newPassword")); return ResponseEntity.ok(Map.of("message","비밀번호 재설정 완료")); }

    @PostMapping("/verify-phone")
    public ResponseEntity<?> verifyPhone(@RequestBody Map<String,String> req) { return ResponseEntity.ok(Map.of("message","인증번호 발송 (Mock)")); }

    @PostMapping("/social/kakao")
    public ResponseEntity<?> kakaoLogin(@RequestBody Map<String,Object> req) {
        String token = socialAuthService.kakaoLogin((String)req.get("accessToken"),(String)req.get("role"),(String)req.get("nickname"),(String)req.get("phone"),(Boolean)req.getOrDefault("marketingAgree",false));
        return ResponseEntity.ok(Map.of("token", token));
    }

    @PostMapping("/social/naver")
    public ResponseEntity<?> naverLogin(@RequestBody Map<String,Object> req) {
        String token = socialAuthService.naverLogin((String)req.get("accessToken"),(String)req.get("role"),(String)req.get("nickname"),(String)req.get("phone"),(Boolean)req.getOrDefault("marketingAgree",false));
        return ResponseEntity.ok(Map.of("token", token));
    }
}
