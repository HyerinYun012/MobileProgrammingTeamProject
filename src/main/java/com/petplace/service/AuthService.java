package com.petplace.service;
import com.petplace.config.JwtUtil;
import com.petplace.dto.request.*;
import com.petplace.entity.*;
import com.petplace.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
@Service @RequiredArgsConstructor @Transactional
public class AuthService {
    private final UserRepository userRepo;
    private final LocalAuthRepository localAuthRepo;
    private final BCryptPasswordEncoder encoder;
    private final JwtUtil jwtUtil;

    public String login(LoginRequest req) {
        LocalAuth la = localAuthRepo.findByLoginId(req.getLoginId())
            .orElseThrow(() -> new IllegalArgumentException("아이디 없음"));
        if (!encoder.matches(req.getPassword(), la.getPassword()))
            throw new IllegalArgumentException("비밀번호 불일치");
        return jwtUtil.generateToken(la.getUser().getId(), la.getUser().getRole().name());
    }

    public void signupCustomer(CustomerSignupRequest req) {
        validate(req.getLoginId(), req.getNickname(), req.getPassword(), req.getPasswordConfirm());
        User u = new User(); u.setName(req.getName()); u.setNickname(req.getNickname());
        u.setPhone(req.getPhone()); u.setRole(User.Role.CUSTOMER); userRepo.save(u);
        saveLocal(u, req.getLoginId(), req.getPassword());
    }

    public void signupOwner(OwnerSignupRequest req) {
        validate(req.getLoginId(), req.getNickname(), req.getPassword(), req.getPasswordConfirm());
        User u = new User(); u.setNickname(req.getNickname()); u.setPhone(req.getPhone());
        u.setMarketingAgree(req.isMarketingAgree()); u.setRole(User.Role.OWNER); userRepo.save(u);
        saveLocal(u, req.getLoginId(), req.getPassword());
    }

    public String findLoginId(String name, String phone) {
        User u = userRepo.findByNameAndPhone(name, phone)
            .orElseThrow(() -> new IllegalArgumentException("일치하는 사용자 없음"));
        LocalAuth la = localAuthRepo.findByUser_Id(u.getId())
            .orElseThrow(() -> new IllegalArgumentException("일반 로그인 계정 없음"));
        String id = la.getLoginId();
        return id.substring(0,2) + "***" + id.substring(id.length()-2);
    }

    public void resetPassword(String loginId, String phone, String newPw) {
        LocalAuth la = localAuthRepo.findByLoginId(loginId)
            .orElseThrow(() -> new IllegalArgumentException("아이디 없음"));
        if (!la.getUser().getPhone().equals(phone))
            throw new IllegalArgumentException("휴대폰 번호 불일치");
        la.setPassword(encoder.encode(newPw));
        localAuthRepo.save(la);
    }

    public boolean isLoginIdExists(String loginId) { return localAuthRepo.existsByLoginId(loginId); }
    public boolean isNicknameExists(String nickname) { return userRepo.existsByNickname(nickname); }

    private void validate(String loginId, String nickname, String pw, String pwConfirm) {
        if (!pw.equals(pwConfirm)) throw new IllegalArgumentException("비밀번호 불일치");
        if (localAuthRepo.existsByLoginId(loginId)) throw new IllegalArgumentException("이미 존재하는 아이디");
        if (userRepo.existsByNickname(nickname)) throw new IllegalArgumentException("이미 존재하는 닉네임");
    }
    private void saveLocal(User user, String loginId, String password) {
        LocalAuth la = new LocalAuth(); la.setUser(user); la.setLoginId(loginId);
        la.setPassword(encoder.encode(password)); localAuthRepo.save(la);
    }
}
