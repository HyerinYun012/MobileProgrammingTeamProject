package com.petplace.repository;

import com.petplace.entity.Bookmark;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface BookmarkRepository extends JpaRepository<Bookmark, Long> {

    /**
     * 💡 [페이징 적용] 전체 조회가 아닌, Pageable을 통한 부분 조회로 변경하여 OOM 방지
     * 호출 시 PageRequest.of(page, size, Sort.by("createdAt").descending())를 전달하세요.
     */
    Page<Bookmark> findAllByUserId(Long userId, Pageable pageable);

    /*
     * 2. [추가] 북마크 토글용: 특정 유저가 특정 식당을 북마크했는지 확인
     */
    Optional<Bookmark> findByUserIdAndRestaurantId(Long userId, Long restaurantId);
}