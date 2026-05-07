package com.petplace.repository;
import com.petplace.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByNameAndPhone(String name, String phone);
    boolean existsByNickname(String nickname);
}
