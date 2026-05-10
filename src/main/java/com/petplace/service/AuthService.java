package com.petplace.service;

import com.petplace.config.JwtUtil;
import com.petplace.dto.request.*;
import com.petplace.entity.*;
import com.petplace.exception.BusinessException;
import com.petplace.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 읽기 전용을 기본으로 설정 (성능 최적화)
public class AuthService {
    private final UserRepository userRepo;
    private final LocalAuthRepository localAuthRepo;
    private final BCryptPasswordEncoder encoder;
    private final JwtUtil jwtUtil;

    /**
     * 로그인: 아이디/비번 검증 후 JWT 반환
     */
    public String login(LoginRequest req) {
        LocalAuth la = localAuthRepo.findByLoginId(req.getLoginId())
                .orElseThrow(() -> new BusinessException("아이디 또는 비밀번호가 일치하지 않습니다."));

        if (!encoder.matches(req.getPassword(), la.getPassword())) {
            throw new BusinessException("아이디 또는 비밀번호가 일치하지 않습니다.");
        }

        // [핵심] JwtUtil을 통해 유저 ID와 Role(OWNER/CUSTOMER)을 토큰에 담아 반환
        return jwtUtil.generateToken(la.getUser().getId(), la.getUser().getRole().name());
    }

    /**
     * 일반 고객 회원가입
     */
    @Transactional // 쓰기 작업이므로 기본 readOnly 설정을 덮어씀
    public void signupCustomer(CustomerSignupRequest req) {
        validate(req.getLoginId(), req.getNickname(), req.getPassword(), req.getPasswordConfirm());

        User u = new User();
        u.setName(req.getName());
        u.setNickname(req.getNickname());
        u.setPhone(req.getPhone());
        u.setRole(User.Role.CUSTOMER);
        userRepo.save(u);

        saveLocal(u, req.getLoginId(), req.getPassword());
    }

    /**
     * 사장님 회원가입
     */
    @Transactional
    public void signupOwner(OwnerSignupRequest req) {
        validate(req.getLoginId(), req.getNickname(), req.getPassword(), req.getPasswordConfirm());

        User u = new User();
        u.setName(req.getName()); // [보완] 사장님 성함도 저장되도록 추가
        u.setNickname(req.getNickname());
        u.setPhone(req.getPhone());
        u.setMarketingAgree(req.isMarketingAgree());
        u.setRole(User.Role.OWNER);
        userRepo.save(u);

        saveLocal(u, req.getLoginId(), req.getPassword());
    }

    /**
     * 아이디 찾기 (마스킹 처리)
     */
    public String findLoginId(String name, String phone) {
        User u = userRepo.findByNameAndPhone(name, phone)
                .orElseThrow(() -> new BusinessException("일치하는 사용자 정보를 찾을 수 없습니다."));

        LocalAuth la = localAuthRepo.findByUser_Id(u.getId())
                .orElseThrow(() -> new BusinessException("일반 로그인으로 가입된 계정이 아닙니다."));

        String id = la.getLoginId();
        if (id.length() < 4) return id; // 짧은 아이디 예외 처리
        return id.substring(0, 2) + "***" + id.substring(id.length() - 2);
    }

    /**
     * 비밀번호 재설정
     */
    @Transactional
    public void resetPassword(String loginId, String phone, String newPw) {
        LocalAuth la = localAuthRepo.findByLoginId(loginId)
                .orElseThrow(() -> new BusinessException("입력하신 정보와 일치하는 계정이 없습니다."));

        if (!la.getUser().getPhone().equals(phone)) {
            throw new BusinessException("계정에 등록된 휴대폰 번호와 일치하지 않습니다.");
        }

        la.setPassword(encoder.encode(newPw));
        // @Transactional에 의해 메서드 종료 시 Dirty Checking으로 자동 업데이트됨
    }

    public boolean isLoginIdExists(String loginId) { return localAuthRepo.existsByLoginId(loginId); }
    public boolean isNicknameExists(String nickname) { return userRepo.existsByNickname(nickname); }

    /**
     * 회원가입 공통 유효성 검사
     */
    private void validate(String loginId, String nickname, String pw, String pwConfirm) {
        if (!pw.equals(pwConfirm))
            throw new BusinessException("비밀번호 확인이 일치하지 않습니다.");
        if (localAuthRepo.existsByLoginId(loginId))
            throw new BusinessException("이미 사용 중인 아이디입니다.");
        if (userRepo.existsByNickname(nickname))
            throw new BusinessException("이미 사용 중인 닉네임입니다.");
    }

    /**
     * 인증 정보 분리 저장 (LocalAuth)
     */
    private void saveLocal(User user, String loginId, String password) {
        LocalAuth la = new LocalAuth();
        la.setUser(user);
        la.setLoginId(loginId);
        la.setPassword(encoder.encode(password));
        localAuthRepo.save(la);
    }
}