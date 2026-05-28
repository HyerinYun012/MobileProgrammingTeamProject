package com.petplace.service;

import com.petplace.dto.response.CommentResponse;
import com.petplace.dto.response.PostDetailResponse;
import com.petplace.entity.*;
import com.petplace.exception.BusinessException;
import com.petplace.exception.ErrorCode;
import com.petplace.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CommunityService {

    private final PostRepository postRepository;
    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final FileService fileService;
    private final CommunityReportRepository communityReportRepository;

    @Transactional
    public void writePost(Long userId, String title, String content, List<String> imageUrls) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Post post = Post.builder()
                .user(user)
                .title(title)
                .content(content)
                .imageUrls(imageUrls != null ? imageUrls : new ArrayList<>())
                .build();

        postRepository.save(post);
    }

    // newImageUrls == null 이면 기존 사진 유지, 비어있지 않은 리스트이면 교체
    @Transactional
    public void updatePost(Long userId, Long postId, String title, String content, List<String> newImageUrls) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        if (!post.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

        List<String> oldImageUrls = new ArrayList<>(post.getImageUrls());
        post.updateContent(title, content, newImageUrls); // null 이면 기존 유지

        // 새 이미지로 교체된 경우 기존 파일 삭제
        if (newImageUrls != null && !oldImageUrls.isEmpty()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    oldImageUrls.forEach(fileService::deleteFile);
                }
            });
        }
    }

    @Transactional
    public void deletePost(Long userId, Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        if (!post.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

        List<String> imageUrls = new ArrayList<>(post.getImageUrls());
        postRepository.delete(post);

        if (!imageUrls.isEmpty()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    imageUrls.forEach(fileService::deleteFile);
                }
            });
        }
    }

    @Transactional
    public void writeComment(Long userId, Long postId, Long parentId, String content) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        if (parentId == null) {
            Comment comment = Comment.createComment(post, user, content);
            commentRepository.save(comment);
        } else {
            Comment parentComment = commentRepository.findById(parentId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.PARENT_COMMENT_NOT_FOUND));
            Comment childComment = Comment.createComment(post, user, content);
            parentComment.addChildComment(childComment);
            commentRepository.save(childComment);
        }
    }

    @Transactional
    public void updateComment(Long commentId, Long userId, String content) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        if (!comment.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }
        comment.updateContent(content);
    }

    @Transactional
    public void deleteComment(Long userId, Long commentId) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        if (!comment.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }
        commentRepository.delete(comment);
    }

    public PostDetailResponse getPostDetail(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));
        return toDetailResponse(post);
    }

    public Page<CommentResponse> getCommentsByPost(Long postId, Long userId, Pageable pageable) {
        Page<Comment> commentPage = commentRepository.findByPostId(postId, pageable);
        return commentPage.map(c -> CommentResponse.from(c, userId));
    }

    public Page<PostDetailResponse> getAllPostsDesc(Pageable pageable) {
        return postRepository.findAllByOrderByCreatedAtDesc(pageable)
                .map(this::toDetailResponse);
    }

    private PostDetailResponse toDetailResponse(Post post) {
        return PostDetailResponse.builder()
                .id(post.getId())
                .userId(post.getUser().getId())
                .writerNickname(post.getUser().getNickname())
                .writerProfileUrl(post.getUser().getProfileUrl())
                .writerRole(post.getUser().getRole() != null ? post.getUser().getRole().name() : "CUSTOMER")
                .title(post.getTitle())
                .content(post.getContent())
                .imageUrls(post.getImageUrls())
                .createdAt(post.getCreatedAt())
                .build();
    }

    @Transactional
    public void reportPost(Long userId, Long postId, String reason) {
        User reporter = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        CommunityReport report = CommunityReport.builder()
                .reporter(reporter)
                .post(post)
                .reason(reason)
                .build();
        communityReportRepository.save(report);
    }

    @Transactional
    public void reportComment(Long userId, Long commentId, String reason) {
        User reporter = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new BusinessException(ErrorCode.COMMENT_NOT_FOUND));

        CommunityReport report = CommunityReport.builder()
                .reporter(reporter)
                .comment(comment)
                .reason(reason)
                .build();
        communityReportRepository.save(report);
    }
}
