package com.petplace.controller;
import com.petplace.dto.request.*;
import com.petplace.service.*;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.Map;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "인증(Auth) API", description = "로그인, 회원가입, 소셜 로그인 등 인증 관련 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final SocialAuthService socialAuthService;

    @Operation(summary = "일반 로그인", description = "일반 회원의 아이디와 비밀번호로 로그인")
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest req) { return ResponseEntity.ok(Map.of("token", authService.login(req))); }

    @Operation(summary = "일반 고객 회원가입", description = "반려인 고객을 위한 회원가입")
    @PostMapping("/signup/customer")
    public ResponseEntity<?> signupCustomer(@RequestBody CustomerSignupRequest req) { authService.signupCustomer(req); return ResponseEntity.ok(Map.of("message","회원가입 완료")); }

    @Operation(summary = "사장님 회원가입", description = "업체 사장님을 위한 회원가입")
    @PostMapping("/signup/owner")
    public ResponseEntity<?> signupOwner(@RequestBody OwnerSignupRequest req) { authService.signupOwner(req); return ResponseEntity.ok(Map.of("message","사장님 가입 완료")); }

    @Operation(summary = "아이디 중복 체크", description = "사용 가능한 아이디인지 확인")
    @GetMapping("/check-id")
    public ResponseEntity<?> checkId(@RequestParam String loginId) { return ResponseEntity.ok(Map.of("available", !authService.isLoginIdExists(loginId))); }

    @Operation(summary = "닉네임 중복 체크")
    @GetMapping("/check-nickname")
    public ResponseEntity<?> checkNickname(@RequestParam String nickname) { return ResponseEntity.ok(Map.of("available", !authService.isNicknameExists(nickname))); }

    @Operation(summary = "아이디 찾기", description = "이름과 전화번호를 통해 아이디를 찾습니다.")
    @PostMapping("/find-id")
    public ResponseEntity<?> findId(@RequestBody Map<String,String> req) { return ResponseEntity.ok(Map.of("loginId", authService.findLoginId(req.get("name"), req.get("phone")))); }

    @Operation(summary = "비밀번호 재설정", description = "본인 인증 후 새 비밀번호로 변경합니다.")
    @PostMapping("/reset-password")
    public ResponseEntity<?> resetPassword(@RequestBody Map<String,String> req) { authService.resetPassword(req.get("loginId"), req.get("phone"), req.get("newPassword")); return ResponseEntity.ok(Map.of("message","비밀번호 재설정 완료")); }

    @Operation(summary = "휴대폰 인증번호 발송", description = "SMS 인증을 위한 목(Mock) API입니다.")
    @PostMapping("/verify-phone")
    public ResponseEntity<?> verifyPhone(@RequestBody Map<String,String> req) { return ResponseEntity.ok(Map.of("message","인증번호 발송 (Mock)")); }

    @Operation(summary = "카카오 소셜 로그인/회원가입", description = "카카오 액세스 토큰으로 로그인을 시도하며, 첫 로그인 시 회원가입 처리됩니다.")
    @PostMapping("/social/kakao")
    public ResponseEntity<?> kakaoLogin(@RequestBody Map<String,Object> req) {
        String token = socialAuthService.kakaoLogin((String)req.get("accessToken"),(String)req.get("role"),(String)req.get("nickname"),(String)req.get("phone"),(Boolean)req.getOrDefault("marketingAgree",false));
        return ResponseEntity.ok(Map.of("token", token));
    }

    @Operation(summary = "네이버 소셜 로그인/회원가입")
    @PostMapping("/social/naver")
    public ResponseEntity<?> naverLogin(@RequestBody Map<String,Object> req) {
        String token = socialAuthService.naverLogin((String)req.get("accessToken"),(String)req.get("role"),(String)req.get("nickname"),(String)req.get("phone"),(Boolean)req.getOrDefault("marketingAgree",false));
        return ResponseEntity.ok(Map.of("token", token));
    }
}
