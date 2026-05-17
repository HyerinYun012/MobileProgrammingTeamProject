package com.petplace.repository;

import com.petplace.entity.Post;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface PostRepository extends JpaRepository<Post, Long> {
    // 커뮤니티 자유게시판: 전체 게시글을 최신 등록순으로 조회합니다.
    List<Post> findAllByOrderByCreatedAtDesc();
}