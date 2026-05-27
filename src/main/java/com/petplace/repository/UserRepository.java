package com.petplace.repository;

import com.petplace.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByPhone(String phone);

    Optional<User> findByNameAndPhone(String name, String phone);

    boolean existsByNickname(String nickname);

    // 🎯 [정상 해결] 가입하려는 이메일이 이미 존재하는지 검증하기 위한 쿼리 메서드 선언
    boolean existsByEmail(String email);

    @EntityGraph(attributePaths = {"localAuth"})
    Page<User> findAllByRoleAndIsVerifiedFalse(User.Role role, Pageable pageable);
}