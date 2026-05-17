package com.petplace.repository;

import com.petplace.entity.Comment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<Comment, Long> {

    /**
     * 🌟 [대적 리팩토링] 특정 게시글의 모든 댓글 및 대댓글을 단 1번의 벌크 쿼리로 일괄 조회합니다.
     * 💡 성능 고도화: 댓글을 작성한 사용자(User) 정보까지 페치 조인(Fetch Join)을 걸어두어,
     * CommentResponse DTO로 변환할 때 작성자 닉네임 등을 조회하며 발생하는 N+1 문제까지 원천 차단합니다.
     */
    @Query("SELECT c FROM Comment c " +
            "JOIN FETCH c.user " +
            "WHERE c.post.id = :postId " +
            "ORDER BY c.createdAt ASC")
    List<Comment> findByPostIdOrderByCreatedAtAsc(@Param("postId") Long postId);
}