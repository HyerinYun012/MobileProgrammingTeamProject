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
    public void writePost(Long userId, String title, String content, String imageUrl) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        Post post = Post.builder()
                .user(user)
                .title(title)
                .content(content)
                .imageUrl(imageUrl)
                .build();

        postRepository.save(post);
    }

    @Transactional
    public void updatePost(Long userId, Long postId, String title, String content, String newImageUrl) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        if (!post.getUser().getId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

        String oldImageUrl = post.getImageUrl();
        post.updateContent(title, content, newImageUrl);

        if (oldImageUrl != null && !oldImageUrl.equals(newImageUrl)) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    fileService.deleteFile(oldImageUrl);
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

        String imageUrl = post.getImageUrl();
        postRepository.delete(post);

        if (imageUrl != null) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    fileService.deleteFile(imageUrl);
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

    // 1. 게시글 상세 조회 (댓글 제외)
    public PostDetailResponse getPostDetail(Long postId) {
        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new BusinessException(ErrorCode.POST_NOT_FOUND));

        return PostDetailResponse.builder()
                .id(post.getId())
                .userId(post.getUser().getId())
                .writerNickname(post.getUser().getNickname())
                .writerProfileUrl(post.getUser().getProfileUrl())
                .title(post.getTitle())
                .content(post.getContent())
                .imageUrl(post.getImageUrl())
                .createdAt(post.getCreatedAt())
                .build();
    }

    // 2. 댓글 목록 조회 (페이징 적용)
    public Page<CommentResponse> getCommentsByPost(Long postId, Long userId, Pageable pageable) {
        Page<Comment> commentPage = commentRepository.findByPostId(postId, pageable);
        return commentPage.map(c -> CommentResponse.from(c, userId));
    }

    public Page<Post> getAllPostsDesc(Pageable pageable) {
        return postRepository.findAllByOrderByCreatedAtDesc(pageable);
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