package com.petplace.repository;

import com.petplace.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    // [추가] 전화번호로 사용자 찾기 (소셜 로그인 계정 통합 시 필요)
    Optional<User> findByPhone(String phone);

    Optional<User> findByNameAndPhone(String name, String phone);

    boolean existsByNickname(String nickname);

    // [참고] 아이디 중복 체크 등에 쓰이는 메서드 (필요시)
    boolean existsByLoginId(String loginId);
}