package com.petplace.controller;

import com.petplace.dto.request.*;
import com.petplace.dto.response.ApiResponse;
import com.petplace.service.*;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Tag(name = "인증(Auth) API", description = "로그인, 회원가입, 소셜 로그인 등 인증 관련 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;
    private final SocialAuthService socialAuthService;

    @Operation(summary = "일반 로그인", description = "일반 회원의 아이디와 비밀번호로 로그인하여 JWT 토큰을 발급받습니다.")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<String>> login(@Valid @RequestBody LoginRequest req) {
        String token = authService.login(req);
        return ResponseEntity.ok(ApiResponse.success("로그인 성공", token));
    }

    @Operation(summary = "일반 고객 회원가입")
    @PostMapping("/signup/customer")
    public ResponseEntity<ApiResponse<Void>> signupCustomer(@Valid @RequestBody CustomerSignupRequest req) {
        authService.signupCustomer(req);
        return ResponseEntity.ok(ApiResponse.success("회원가입이 완료되었습니다.", null));
    }

    @Operation(summary = "사장님 회원가입")
    @PostMapping("/signup/owner")
    public ResponseEntity<ApiResponse<Void>> signupOwner(@Valid @RequestBody OwnerSignupRequest req) {
        authService.signupOwner(req);
        return ResponseEntity.ok(ApiResponse.success("사장님 가입 신청이 완료되었습니다.", null));
    }

    @Operation(summary = "아이디 중복 체크")
    @GetMapping("/check-id")
    public ResponseEntity<ApiResponse<Boolean>> checkId(@RequestParam String loginId) {
        boolean isAvailable = !authService.isLoginIdExists(loginId);
        return ResponseEntity.ok(ApiResponse.success(isAvailable));
    }

    @Operation(summary = "닉네임 중복 체크")
    @GetMapping("/check-nickname")
    public ResponseEntity<ApiResponse<Boolean>> checkNickname(@RequestParam String nickname) {
        boolean isAvailable = !authService.isNicknameExists(nickname);
        return ResponseEntity.ok(ApiResponse.success(isAvailable));
    }

    @Operation(summary = "아이디 찾기")
    @PostMapping("/find-id")
    public ResponseEntity<ApiResponse<String>> findId(@Valid @RequestBody FindIdRequest req) {
        String loginId = authService.findLoginId(req.getName(), req.getPhone());
        return ResponseEntity.ok(ApiResponse.success("아이디 찾기 성공", loginId));
    }

    @Operation(summary = "비밀번호 재설정")
    @PostMapping("/reset-password")
    public ResponseEntity<ApiResponse<Void>> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        authService.resetPassword(req.getLoginId(), req.getPhone(), req.getNewPassword());
        return ResponseEntity.ok(ApiResponse.success("비밀번호가 안전하게 변경되었습니다.", null));
    }

    @Operation(summary = "소셜 로그인", description = "소셜 액세스 토큰으로 가입 여부를 확인하고, 기존 회원이면 JWT 토큰을 발급합니다.")
    @PostMapping("/social/login")
    public ResponseEntity<ApiResponse<String>> socialLogin(@Valid @RequestBody SocialLoginRequest req) {
        String token = socialAuthService.login(req);
        return ResponseEntity.ok(ApiResponse.success("로그인 성공", token));
    }

    @Operation(summary = "소셜 회원가입", description = "신규 소셜 유저가 추가 정보를 입력하여 최종 회원가입을 완료합니다.")
    @PostMapping("/social/signup")
    public ResponseEntity<ApiResponse<String>> socialSignup(@Valid @RequestBody SocialSignupRequest req) {
        String token = socialAuthService.signup(req);
        return ResponseEntity.ok(ApiResponse.success("회원가입 및 로그인 성공", token));
    }
}