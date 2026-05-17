package com.petplace.controller;

import com.petplace.dto.response.ApiResponse;
import com.petplace.dto.response.CommentResponse;
import com.petplace.dto.response.PostDetailResponse; // 💡 신규 게시글 상세 응답 DTO 임포트
import com.petplace.entity.Post;
import com.petplace.service.CommunityService;
import com.petplace.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Tag(name = "2. Community API", description = "사용자 커뮤니티 게시글, 댓글 관리 및 신고 API")
@RestController
@RequestMapping("/api/community")
@RequiredArgsConstructor
public class CommunityController {

    private final CommunityService communityService;
    private final FileService fileService;

    /**
     * 자유게시판 전체 게시글 최신순 조회 (로그인 필수)
     */
    @Operation(summary = "커뮤니티 전체 게시글 최신순 조회", description = "자유게시판의 모든 게시글 목록을 최신 등록순으로 조회합니다. (로그인한 회원만 접근 가능)")
    @GetMapping("/posts")
    public ResponseEntity<ApiResponse<List<Post>>> getAllPosts() {
        // SecurityConfig가 인증을 강제하므로, 이 안으로 들어왔다는 것 자체가 이미 로그인 상태임을 보장합니다.
        List<Post> posts = communityService.getAllPostsDesc();
        return ResponseEntity.ok(ApiResponse.success("게시글 목록이 성공적으로 조회되었습니다.", posts));
    }

    /**
     * 💡 [신규 추가] 커뮤니티 게시글 단건 상세 조회 (게시글 본문 + 댓글 트리 통합 통합본)
     * 이 엔드포인트가 연결되면서 PostDetailResponse 클래스의 미사용 경고가 완벽히 사라집니다.
     */
    @Operation(summary = "커뮤니티 게시글 상세 조회", description = "특정 게시글의 상세 정보와 함께 로그인한 회원의 댓글 본인 여부(isMine)가 매핑된 댓글/대댓글 트리를 일괄 조회합니다.")
    @GetMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<PostDetailResponse>> getPostDetail(
            @Parameter(description = "상세 조회할 게시글 ID", example = "10") @PathVariable Long postId,
            @AuthenticationPrincipal Long currentUserId) {

        PostDetailResponse response = communityService.getPostDetail(postId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("게시글 상세 조회가 완료되었습니다.", response));
    }

    /**
     * 커뮤니티 게시글 작성
     */
    @Operation(summary = "커뮤니티 게시글 작성", description = "인증된 사용자가 이미지 파일과 내용을 조합하여 게시글을 작성합니다.")
    @PostMapping(value = "/posts", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> writePost(
            @AuthenticationPrincipal Long currentUserId,
            @Parameter(description = "게시글 제목", example = "우리집 댕댕이 자랑합니다") @RequestParam String title,
            @Parameter(description = "게시글 본문 내용", example = "너무 귀엽지 않나요?") @RequestParam String content,
            @Parameter(description = "첨부 이미지 파일 (선택)")
            @RequestPart(value = "image", required = false) MultipartFile image) throws IOException {

        String imageUrl = uploadIfPresent(image);

        communityService.writePost(currentUserId, title, content, imageUrl);
        return ResponseEntity.ok(ApiResponse.success("게시글이 성공적으로 등록되었습니다.", null));
    }

    /**
     * 커뮤니티 게시글 수정
     */
    @Operation(summary = "커뮤니티 게시글 수정", description = "본인이 작성한 게시글을 수정하며, 이미지 변경 시 기존 S3 파일은 트랜잭션 성공 후 삭제 처리됩니다.")
    @PutMapping(value = "/posts/{postId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> updatePost(
            @Parameter(description = "수정할 게시글 ID", example = "10") @PathVariable Long postId,
            @AuthenticationPrincipal Long currentUserId,
            @Parameter(description = "변경할 게시글 제목") @RequestParam String title,
            @Parameter(description = "변경할 게시글 본문 내용") @RequestParam String content,
            @Parameter(description = "새로운 첨부 이미지 파일 (선택)")
            @RequestPart(value = "image", required = false) MultipartFile image) throws IOException {

        String newImageUrl = uploadIfPresent(image);

        communityService.updatePost(postId, currentUserId, title, content, newImageUrl);
        return ResponseEntity.ok(ApiResponse.success("게시글이 성공적으로 수정되었습니다.", null));
    }

    /**
     * 커뮤니티 게시글 삭제
     */
    @Operation(summary = "커뮤니티 게시글 삭제", description = "본인이 작성한 게시글을 삭제하며, 연동된 S3 파일도 함께 제거됩니다.")
    @DeleteMapping("/posts/{postId}")
    public ResponseEntity<ApiResponse<Void>> deletePost(
            @Parameter(description = "삭제할 게시글 ID", example = "10") @PathVariable Long postId,
            @AuthenticationPrincipal Long currentUserId) {

        communityService.deletePost(postId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("게시글이 성공적으로 삭제되었습니다.", null));
    }

    /**
     * 댓글 및 대댓글 작성
     */
    @Operation(summary = "댓글 및 대댓글 작성", description = "인증된 사용자가 특정 게시글에 댓글 또는 대댓글을 작성합니다.")
    @PostMapping("/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<Void>> writeComment(
            @Parameter(description = "게시글 ID", example = "10") @PathVariable Long postId,
            @AuthenticationPrincipal Long currentUserId,
            @Parameter(description = "부모 댓글 ID (대댓글인 경우에만 입력)", example = "1")
            @RequestParam(required = false) Long parentId,
            @Parameter(description = "댓글 내용", example = "정말 귀엽네요!") @RequestParam String content) {

        communityService.writeComment(postId, currentUserId, parentId, content);
        return ResponseEntity.ok(ApiResponse.success("댓글이 등록되었습니다.", null));
    }

    /**
     * 댓글 수정
     */
    @Operation(summary = "댓글 수정", description = "본인이 작성한 댓글의 내용을 수정합니다.")
    @PutMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> updateComment(
            @Parameter(description = "수정할 댓글 ID", example = "25") @PathVariable Long commentId,
            @AuthenticationPrincipal Long currentUserId,
            @Parameter(description = "수정할 댓글 내용", example = "다시 보니 더 귀엽네요!") @RequestParam String content) {

        communityService.updateComment(commentId, currentUserId, content);
        return ResponseEntity.ok(ApiResponse.success("댓글이 성공적으로 수정되었습니다.", null));
    }

    /**
     * 댓글 삭제
     */
    @Operation(summary = "댓글 삭제", description = "본인이 작성한 댓글을 삭제합니다. 원댓글 삭제 시 대댓글도 함께 연쇄 삭제됩니다.")
    @DeleteMapping("/comments/{commentId}")
    public ResponseEntity<ApiResponse<Void>> deleteComment(
            @Parameter(description = "삭제할 댓글 ID", example = "25") @PathVariable Long commentId,
            @AuthenticationPrincipal Long currentUserId) {

        communityService.deleteComment(commentId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("댓글이 성공적으로 삭제되었습니다.", null));
    }

    /**
     * 게시글별 댓글 목록 조회
     */
    @Operation(summary = "게시글별 댓글 목록 조회", description = "인증된 회원만 특정 게시글의 댓글 및 대댓글 목록을 조회할 수 있습니다.")
    @GetMapping("/posts/{postId}/comments")
    public ResponseEntity<ApiResponse<List<CommentResponse>>> getComments(
            @Parameter(description = "조회할 게시글 ID", example = "10") @PathVariable Long postId,
            @AuthenticationPrincipal Long currentUserId) {

        List<CommentResponse> response = communityService.getCommentsByPost(postId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("댓글 목록이 성공적으로 조회되었습니다.", response));
    }

    /**
     * 커뮤니티 게시글 신고하기
     */
    @Operation(summary = "커뮤니티 게시글 신고", description = "일반 유저가 유해하거나 규칙을 위반한 게시글을 신고합니다.")
    @PostMapping("/posts/{postId}/report")
    public ResponseEntity<ApiResponse<Void>> reportPost(
            @Parameter(description = "신고 대상 게시글 ID", example = "10") @PathVariable Long postId,
            @AuthenticationPrincipal Long currentUserId,
            @Parameter(description = "신고 사유", example = "광고성 도배 글입니다.") @RequestParam String reason) {

        communityService.reportPost(currentUserId, postId, reason);
        return ResponseEntity.ok(ApiResponse.success("게시글 신고가 접수되었습니다.", null));
    }

    /**
     * 커뮤니티 댓글 신고하기
     */
    @Operation(summary = "커뮤니티 댓글 신고", description = "일반 유저가 유해하거나 규칙을 위반한 댓글을 신고합니다.")
    @PostMapping("/comments/{commentId}/report")
    public ResponseEntity<ApiResponse<Void>> reportComment(
            @Parameter(description = "신고 대상 댓글 ID", example = "25") @PathVariable Long commentId,
            @AuthenticationPrincipal Long currentUserId,
            @Parameter(description = "신고 사유", example = "욕설 및 비방이 포함되어 있습니다.") @RequestParam String reason) {

        communityService.reportComment(currentUserId, commentId, reason);
        return ResponseEntity.ok(ApiResponse.success("댓글 신고가 접수되었습니다.", null));
    }

    /**
     * 파일이 존재할 경우에만 유효성(MIME 타입/확장자)을 검증한 뒤 S3에 업로드하고 주소를 반환합니다.
     */
    private String uploadIfPresent(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty()) {
            return null;
        }

        // 1. Content-Type (MIME 타입) 검증을 통한 일차 방어
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("이미지 파일(jpg, jpeg, png, gif, webp)만 업로드할 수 있습니다.");
        }

        // 2. 확장자 검사를 통한 이차 복합 방어
        String originalFilename = file.getOriginalFilename();
        if (originalFilename == null || originalFilename.isBlank()) {
            throw new IllegalArgumentException("올바르지 않은 파일명입니다.");
        }

        // 💡 [메서드 추출] 하단에 정의한 헬퍼 메서드를 호출하여 확장자 유효성 검증
        if (!isValidExtension(originalFilename)) {
            throw new IllegalArgumentException("허용되지 않는 파일 확장자입니다. 이미지 파일만 등록해 주세요.");
        }

        // 모든 방어선을 통과하면 S3 업로드 진행
        return fileService.uploadFile(file);
    }

    /**
     * 💡 [IntelliJ 경고 해결] 파일명의 확장자가 허용된 이미지 포맷인지 확인하는 전용 검증 메서드
     */
    private boolean isValidExtension(String filename) {
        String lowerFilename = filename.toLowerCase();
        return lowerFilename.endsWith(".jpg") ||
                lowerFilename.endsWith(".jpeg") ||
                lowerFilename.endsWith(".png") ||
                lowerFilename.endsWith(".gif") ||
                lowerFilename.endsWith(".webp");
    }
}