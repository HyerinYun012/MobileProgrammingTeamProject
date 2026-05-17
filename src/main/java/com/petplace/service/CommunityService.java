package com.petplace.service;

import com.petplace.dto.response.CommentResponse;
import com.petplace.dto.response.PostDetailResponse;
import com.petplace.entity.*;
import com.petplace.exception.BusinessException;
import com.petplace.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true) // 기본은 읽기 전용 성능 최적화 적용
public class CommunityService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final FileService fileService;
    private final CommunityReportRepository communityReportRepository;

    /**
     * 커뮤니티 게시글 작성
     */
    @Transactional
    public void writePost(Long userId, String title, String content, String imageUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다."));

        // @SuperBuilder 명세에 맞춰 빌더 패턴으로 안전하게 객체를 생성합니다.
        Post post = Post.builder()
                .user(user)
                .title(title)
                .content(content)
                .imageUrl(imageUrl)
                .build();

        postRepository.save(post);
    }

    /**
     * 커뮤니티 게시글 수정
     */
    @Transactional
    public void updatePost(Long postId, Long userId, String title, String content, String newImageUrl) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException("존재하지 않는 게시글입니다."));

        if (post.getUser() == null || !Objects.equals(post.getUser().getId(), userId)) {
            throw new BusinessException("해당 게시글을 수정할 권한이 없습니다.");
        }

        if (post.getImageUrl() != null && !post.getImageUrl().isEmpty() &&
                !Objects.equals(post.getImageUrl(), newImageUrl)) {

            String oldImageUrl = post.getImageUrl();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    fileService.deleteFile(oldImageUrl);
                }
            });
        }

        // 🌟 [수정 완료] 개별 Setter 분출 구조를 폐쇄하고, Post 엔티티 내부의 전용 도메인 메서드를 호출합니다.
        // 이를 통해 객체 스스로가 상태 변경을 제어하는 원자성을 가집니다.
        post.updateContent(title, content, newImageUrl);
    }

    /**
     * 커뮤니티 게시글 삭제 (안전한 S3 파일 삭제 연동 버전)
     */
    @Transactional
    public void deletePost(Long postId, Long userId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException("존재하지 않는 게시글입니다."));

        if (post.getUser() == null || !Objects.equals(post.getUser().getId(), userId)) {
            throw new BusinessException("해당 게시글을 삭제할 권한이 없습니다.");
        }

        if (post.getImageUrl() != null && !post.getImageUrl().isEmpty()) {
            String targetImageUrl = post.getImageUrl();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    fileService.deleteFile(targetImageUrl);
                }
            });
        }

        postRepository.delete(post);
    }

    /**
     * 커뮤니티 게시글 상세 조회 (게시글 + 댓글 트리 통합본)
     */
    public PostDetailResponse getPostDetail(Long postId, Long loginUserId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException("존재하지 않는 게시글입니다."));

        // 메모리 트리 조립 기반 고성능 로직 호출
        List<CommentResponse> comments = getCommentsByPost(postId, loginUserId);

        return new PostDetailResponse(
                post.getId(),
                post.getTitle(),
                post.getContent(),
                post.getImageUrl(),
                post.getUser() != null ? post.getUser().getNickname() : "알 수 없는 사용자",
                post.getUser() != null ? post.getUser().getProfileUrl() : null,
                post.getCreatedAt(),
                comments
        );
    }

    /**
     * 댓글 및 대댓글 작성
     */
    @Transactional
    public void writeComment(Long postId, Long userId, Long parentId, String content) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException("게시글을 찾을 수 없습니다."));
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다."));

        Comment comment = new Comment();
        comment.setPost(post);
        comment.setUser(user);
        comment.setContent(content);

        if (parentId != null) {
            Comment parent = commentRepository.findById(parentId)
                    .orElseThrow(() -> new BusinessException("부모 댓글을 찾을 수 없습니다."));
            comment.setParent(parent);
        }

        commentRepository.save(comment);
    }

    /**
     * 댓글 수정
     */
    @Transactional
    public void updateComment(Long commentId, Long userId, String content) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException("존재하지 않는 댓글입니다."));

        if (comment.getUser() == null || !Objects.equals(comment.getUser().getId(), userId)) {
            throw new BusinessException("해당 댓글을 수정할 권한이 없습니다.");
        }

        comment.setContent(content);
    }

    /**
     * 댓글 삭제 (대댓글 자동 연쇄 삭제)
     */
    @Transactional
    public void deleteComment(Long commentId, Long userId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException("존재하지 않는 댓글입니다."));

        if (comment.getUser() == null || !Objects.equals(comment.getUser().getId(), userId)) {
            throw new BusinessException("해당 댓글을 삭제할 권한이 없습니다.");
        }

        commentRepository.delete(comment);
    }

    /**
     * 특정 게시글의 댓글 목록 조회 (메모리 조립형 비즈니스 아키텍처)
     */
    public List<CommentResponse> getCommentsByPost(Long postId, Long loginUserId) {
        userRepository.findById(loginUserId)
                .orElseThrow(() -> new BusinessException("인증되지 않은 사용자입니다. 로그인 후 이용해주세요."));
        postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException("게시글을 찾을 수 없습니다."));

        List<Comment> allComments = commentRepository.findByPostIdOrderByCreatedAtAsc(postId);

        Map<Long, CommentResponse> responseMap = new LinkedHashMap<>();
        List<CommentResponse> rootComments = new ArrayList<>();

        for (Comment comment : allComments) {
            CommentResponse response = new CommentResponse(comment, loginUserId);
            responseMap.put(response.getId(), response);

            if (comment.getParent() == null) {
                rootComments.add(response);
            }
        }

        for (Comment comment : allComments) {
            if (comment.getParent() != null) {
                CommentResponse parentResponse = responseMap.get(comment.getParent().getId());
                CommentResponse childResponse = responseMap.get(comment.getId());

                if (parentResponse != null && childResponse != null) {
                    parentResponse.getChildren().add(childResponse);
                }
            }
        }

        return rootComments;
    }

    /**
     * 자유게시판 전체 게시글 최신순 조회
     */
    public List<Post> getAllPostsDesc() {
        return postRepository.findAllByOrderByCreatedAtDesc();
    }

    /**
     * 커뮤니티 게시글 신고 (데이터 무결성 검증 포함)
     */
    @Transactional
    public void reportPost(Long userId, Long postId, String reason) {
        User reporter = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다."));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException("존재하지 않는 게시글입니다."));

        CommunityReport report = CommunityReport.builder()
                .reporter(reporter)
                .post(post)
                .comment(null)
                .reason(reason)
                .build();

        communityReportRepository.save(report);
    }

    /**
     * 커뮤니티 댓글 신고 (데이터 무결성 검증 포함)
     */
    @Transactional
    public void reportComment(Long userId, Long commentId, String reason) {
        User reporter = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException("사용자를 찾을 수 없습니다."));

        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException("존재하지 않는 댓글입니다."));

        CommunityReport report = CommunityReport.builder()
                .reporter(reporter)
                .post(null)
                .comment(comment)
                .reason(reason)
                .build();

        communityReportRepository.save(report);
    }
}