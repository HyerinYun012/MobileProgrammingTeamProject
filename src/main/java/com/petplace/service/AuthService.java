package com.petplace.service;

import com.petplace.config.JwtUtil;
import com.petplace.dto.request.*;
import com.petplace.entity.*;
import com.petplace.exception.BusinessException;
import com.petplace.exception.ErrorCode; // 💡 import 추가
import com.petplace.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {
    private final UserRepository userRepo;
    private final LocalAuthRepository localAuthRepo;
    private final BCryptPasswordEncoder encoder;
    private final JwtUtil jwtUtil;

    public String login(LoginRequest req) {
        LocalAuth la = localAuthRepo.findByLoginId(req.getLoginId())
                .orElseThrow(() -> new BusinessException(ErrorCode.INVALID_LOGIN_INFO));

        if (!encoder.matches(req.getPassword(), la.getPassword())) {
            throw new BusinessException(ErrorCode.INVALID_LOGIN_INFO);
        }

        return jwtUtil.generateToken(la.getUser().getId(), la.getUser().getRole().name());
    }

    @Transactional
    public void signupCustomer(@NonNull CustomerSignupRequest req) {
        validate(req.getLoginId(), req.getNickname(), req.getEmail(), req.getPassword(), req.getPasswordConfirm());

        User user = User.builder()
                .nickname(req.getNickname())
                .name(req.getName())
                .email(req.getEmail())
                .phone(req.getPhone())
                .role(User.Role.CUSTOMER)
                .build();
        userRepo.save(user);

        LocalAuth la = LocalAuth.createLocalAuth(user, req.getLoginId(), encoder.encode(req.getPassword()));
        localAuthRepo.save(la);
    }

    @Transactional
    public void signupOwner(OwnerSignupRequest req) {
        validate(req.getLoginId(), req.getNickname(), req.getEmail(), req.getPassword(), req.getPasswordConfirm());

        User user = User.builder()
                .nickname(req.getNickname())
                .name(req.getName())
                .email(req.getEmail())
                .phone(req.getPhone())
                .role(User.Role.OWNER)
                .isVerified(false)
                .build();
        userRepo.save(user);

        LocalAuth la = LocalAuth.createLocalAuth(user, req.getLoginId(), encoder.encode(req.getPassword()));
        localAuthRepo.save(la);
    }

    @Transactional
    public void resetPassword(String loginId, String phone, String newPw) {
        LocalAuth la = localAuthRepo.findByLoginId(loginId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        if (!la.getUser().getPhone().equals(phone)) {
            throw new BusinessException(ErrorCode.INVALID_PHONE_NUMBER);
        }

        la.changePassword(encoder.encode(newPw));
    }

    public String findLoginId(String name, String phone) {
        // 1. 이름과 전화번호로 User 조회
        User user = userRepo.findByNameAndPhone(name, phone)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        // 2. 해당 User와 연결된 LocalAuth 조회
        LocalAuth localAuth = localAuthRepo.findByUser(user)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        return localAuth.getLoginId();
    }

    public boolean isLoginIdExists(String loginId) { return localAuthRepo.existsByLoginId(loginId); }
    public boolean isNicknameExists(String nickname) { return userRepo.existsByNickname(nickname); }

    private void validate(String loginId, String nickname, String email, String pw, String pwConfirm) {
        if (pw == null || !pw.equals(pwConfirm)) {
            throw new BusinessException(ErrorCode.PASSWORD_NOT_MATCH);
        }
        if (localAuthRepo.existsByLoginId(loginId)) {
            throw new BusinessException(ErrorCode.DUPLICATE_LOGIN_ID);
        }
        if (userRepo.existsByNickname(nickname)) {
            throw new BusinessException(ErrorCode.DUPLICATE_NICKNAME);
        }
        if (userRepo.existsByEmail(email)) {
            throw new BusinessException(ErrorCode.DUPLICATE_EMAIL);
        }
    }
}