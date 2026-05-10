package com.petplace.repository;
import com.petplace.entity.LocalAuth;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;
public interface LocalAuthRepository extends JpaRepository<LocalAuth, Long> {
    Optional<LocalAuth> findByLoginId(String loginId);
    Optional<LocalAuth> findByUser_Id(Long userId);
    boolean existsByLoginId(String loginId);
}
