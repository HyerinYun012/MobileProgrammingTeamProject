package com.petplace.controller;

import com.petplace.dto.request.*;
import com.petplace.dto.response.ApiResponse;
import com.petplace.dto.response.CommentResponse;
import com.petplace.dto.response.PostDetailResponse;
import com.petplace.service.CommunityService;
import com.petplace.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "2. Community API", description = "사용자 커뮤니티 게시글, 댓글 관리 및 신고 API")
@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;
    private final FileService fileService;

    @Operation(summary = "커뮤니티 전체 게시글 최신순 조회")
    @GetMapping("/posts")
    public ResponseEntity<ApiResponse<Page<PostDetailResponse>>> getAllPosts(
            @org.springdoc.core.annotations.ParameterObject
            @PageableDefault(page = 0, size = 1, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        if (pageable.getSort().stream().anyMatch(order -> "string".equals(order.getProperty()))) {
            pageable = org.springframework.data.domain.PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    org.springframework.data.domain.Sort.by("createdAt").descending()
            );
        }

        return ResponseEntity.ok(ApiResponse.success("조회 성공", communityService.getAllPostsDesc(pageable)));
    }

    @Operation(summary = "커뮤니티 게시글 상세 조회")
    @GetMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<PostDetailResponse>> getPostDetail(
            @PathVariable Long postId,
            @AuthenticationPrincipal Long currentUserId) {
        return ResponseEntity.ok(ApiResponse.success("조회 성공", communityService.getPostDetail(postId)));
    }

    @Operation(summary = "커뮤니티 게시글 작성", description = "텍스트(data 파트, JSON)와 이미지 파일(image 파트)을 분리하여 전송합니다.")
    @PostMapping(value = "/posts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> writePost(
            @AuthenticationPrincipal Long currentUserId,
            @Valid @RequestPart("data") PostRequest req,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        communityService.writePost(currentUserId, req.title(), req.content(), uploadIfPresent(image));
        return ResponseEntity.ok(ApiResponse.success("작성 성공", null));
    }

    @Operation(summary = "커뮤니티 게시글 수정", description = "본인이 작성한 글을 수정합니다. 텍스트(data 파트, JSON)와 이미지 파일(image 파트)을 분리하여 전송합니다.")
    @PutMapping(value = "/posts/{postId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> updatePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal Long currentUserId,
            @Valid @RequestPart("data") PostRequest req,
            @RequestPart(value = "image", required = false) MultipartFile image) {
        communityService.updatePost(currentUserId, postId, req.title(), req.content(), uploadIfPresent(image));
        return ResponseEntity.ok(ApiResponse.success("수정 성공", null));
    }

    @Operation(summary = "커뮤니티 게시글 삭제")
    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @PathVariable Long postId,
            @AuthenticationPrincipal Long currentUserId) {
        communityService.deletePost(currentUserId, postId);
        return ResponseEntity.ok(ApiResponse.success("삭제 성공", null));
    }

    @Operation(summary = "댓글 및 대댓글 작성", description = "댓글을 등록합니다.")
    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<Void>> writeComment(
            @PathVariable Long postId,
            @AuthenticationPrincipal Long currentUserId,
            @Valid @RequestBody CommentRequest req) {
        communityService.writeComment(currentUserId, postId, req.parentId(), req.content());
        return ResponseEntity.ok(ApiResponse.success("댓글 등록 성공", null));
    }

    @Operation(summary = "댓글 수정", description = "댓글 내용을 수정합니다.")
    @PutMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> updateComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal Long currentUserId,
            @Valid @RequestBody CommentRequest req) {
        communityService.updateComment(commentId, currentUserId, req.content());
        return ResponseEntity.ok(ApiResponse.success("수정 성공", null));
    }

    @Operation(summary = "댓글 삭제")
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal Long currentUserId) {
        communityService.deleteComment(currentUserId, commentId);
        return ResponseEntity.ok(ApiResponse.success("삭제 성공", null));
    }

    @Operation(summary = "게시글별 댓글 목록 조회")
    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<Page<CommentResponse>>> getComments(
            @PathVariable Long postId,
            @AuthenticationPrincipal Long currentUserId,
            @org.springdoc.core.annotations.ParameterObject
            @PageableDefault(page = 0, size = 1, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        if (pageable.getSort().stream().anyMatch(order -> "string".equals(order.getProperty()))) {
            pageable = org.springframework.data.domain.PageRequest.of(
                    pageable.getPageNumber(),
                    pageable.getPageSize(),
                    org.springframework.data.domain.Sort.by("createdAt").descending()
            );
        }

        return ResponseEntity.ok(ApiResponse.success("조회 성공", communityService.getCommentsByPost(postId, currentUserId, pageable)));
    }

    @Operation(summary = "커뮤니티 게시글 신고", description = "사유를 기반으로 게시글을 신고합니다.")
    @PostMapping("/posts/{postId}/report")
    public ResponseEntity<ApiResponse<Void>> reportPost(
            @PathVariable Long postId,
            @AuthenticationPrincipal Long currentUserId,
            @Valid @RequestBody CommunityReportRequest req) { // 💡 신고 규격도 @RequestBody JSON으로 변경
        communityService.reportPost(currentUserId, postId, req.reason());
        return ResponseEntity.ok(ApiResponse.success("신고 성공", null));
    }

    @Operation(summary = "커뮤니티 댓글 신고", description = "사유를 기반으로 댓글을 신고합니다.")
    @PostMapping("/comments/{commentId}/report")
    public ResponseEntity<ApiResponse<Void>> reportComment(
            @PathVariable Long commentId,
            @AuthenticationPrincipal Long currentUserId,
            @Valid @RequestBody CommunityReportRequest req) {
        communityService.reportComment(currentUserId, commentId, req.reason());
        return ResponseEntity.ok(ApiResponse.success("신고 성공", null));
    }

    private String uploadIfPresent(MultipartFile file) {
        if (file == null || file.isEmpty()) return null;
        if (file.getContentType() == null || !file.getContentType().startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일만 가능합니다.");
        }
        return fileService.uploadFile(file);
    }
}