package com.petplace.repository;

import com.petplace.entity.Post;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {

    /**
     * 💡 [페이징 적용]
     * 게시글 목록을 한 번에 다 가져오지 않고, Pageable을 통해 필요한 만큼만 끊어서 조회합니다.
     * * @param pageable 페이징 정보 (페이지 번호, 사이즈, 정렬)
     * @return Page<Post> 페이징된 게시글 목록
     */
    Page<Post> findAllByOrderByCreatedAtDesc(Pageable pageable);
}