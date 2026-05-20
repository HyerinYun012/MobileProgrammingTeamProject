package com.petplace.controller;

import com.petplace.dto.response.ApiResponse;
import com.petplace.dto.response.NoticeResponse;
import com.petplace.service.NoticeService;
import com.petplace.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "3. Notice API", description = "가게 사장님의 매장별 소식 및 공지사항 관리 API (CRUD)")
@RestController
@RequestMapping("/api/restaurants")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;
    private final FileService fileService;

    /**
     * 식당별 공지사항 목록 조회 (페이징 적용)
     */
    @Operation(summary = "식당별 공지사항 목록 조회", description = "특정 식당에 등록된 모든 공지사항을 최신 등록순으로 페이징 조회합니다.")
    @GetMapping("/{restaurantId}/notices")
    public ResponseEntity<ApiResponse<Page<NoticeResponse>>> getNotices(
            @Parameter(description = "조회할 식당 고유 ID", example = "1") @PathVariable Long restaurantId,
            // 💡 페이징 파라미터 추가 (기본값 10개, 생성일 기준 내림차순)
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        // 💡 서비스 계층의 변경된 메서드에 맞춰 pageable을 전달합니다.
        Page<NoticeResponse> notices = noticeService.getNoticesByRestaurant(restaurantId, pageable);

        return ResponseEntity.ok(ApiResponse.success("공지사항 목록이 성공적으로 조회되었습니다.", notices));
    }

    /**
     * 가게 공지사항 신규 등록
     */
    @Operation(summary = "가게 공지사항 신규 등록", description = "대표 이미지 파일, 본문 설명 이미지 파일을 조합하여 새로운 가게 공지를 등록합니다.")
    @PostMapping(value = "/{restaurantId}/notices", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> createNotice(
            @Parameter(description = "식당 고유 ID", example = "1") @PathVariable Long restaurantId,
            @AuthenticationPrincipal Long currentUserId,
            @Parameter(description = "공지사항 제목", example = "이번 주말 임시 휴업 안내") @RequestParam String title,
            @Parameter(description = "공지사항 본문 내용", example = "내부 인테리어 공사로 인해 이번 주 토요일은 휴무합니다.") @RequestParam String content,
            @Parameter(description = "공지 목록에 노출될 썸네일 대표 사진 파일 (선택)")
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail,
            @Parameter(description = "공지 본문 내 삽입될 설명 이미지 파일 (선택)")
            @RequestPart(value = "descriptionImage", required = false) MultipartFile descriptionImage) { // 💡 throws IOException 추가

        String thumbnailUrl = uploadIfPresent(thumbnail);
        String descriptionImageUrl = uploadIfPresent(descriptionImage);

        noticeService.createNotice(restaurantId, currentUserId, title, content, thumbnailUrl, descriptionImageUrl);
        return ResponseEntity.ok(ApiResponse.success("공지사항이 등록되었습니다.", null));
    }

    /**
     * 가게 공지사항 수정
     */
    @Operation(summary = "가게 공지사항 수정", description = "이미 등록된 공지사항의 정보 및 새로운 이미지 파일들로 변경합니다. (파일 미첨부 시 기존 이미지 유지)")
    @PutMapping(value = "/notices/{noticeId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Void>> updateNotice(
            @Parameter(description = "수정할 공지사항 ID", example = "5") @PathVariable Long noticeId,
            @AuthenticationPrincipal Long currentUserId,
            @Parameter(description = "수정할 공지 제목") @RequestParam String title,
            @Parameter(description = "수정할 공지 본문 내용") @RequestParam String content,
            @Parameter(description = "수정할 썸네일 이미지 파일 (선택)")
            @RequestPart(value = "thumbnail", required = false) MultipartFile thumbnail,
            @Parameter(description = "수정할 설명용 이미지 파일 (선택)")
            @RequestPart(value = "descriptionImage", required = false) MultipartFile descriptionImage) { // 💡 throws IOException 추가

        String thumbnailUrl = uploadIfPresent(thumbnail);
        String descriptionImageUrl = uploadIfPresent(descriptionImage);

        noticeService.updateNotice(noticeId, currentUserId, title, content, thumbnailUrl, descriptionImageUrl);
        return ResponseEntity.ok(ApiResponse.success("공지사항이 수정되었습니다.", null));
    }

    /**
     * 가게 공지사항 삭제
     */
    @Operation(summary = "가게 공지사항 삭제", description = "사장님 검증을 거친 후 등록되어 있던 공지사항을 제거합니다.")
    @DeleteMapping("/notices/{noticeId}")
    public ResponseEntity<ApiResponse<Void>> deleteNotice(
            @Parameter(description = "삭제할 공지사항 ID", example = "5") @PathVariable Long noticeId,
            @AuthenticationPrincipal Long currentUserId) {

        noticeService.deleteNotice(noticeId, currentUserId);
        return ResponseEntity.ok(ApiResponse.success("공지사항이 성공적으로 삭제되었습니다.", null));
    }

    /**
     * 파일이 존재할 경우에만 S3에 업로드하고 주소를 반환합니다.
     */
    private String uploadIfPresent(MultipartFile file) { // 💡 throws IOException 추가
        if (file != null && !file.isEmpty()) {
            return fileService.uploadFile(file);
        }
        return null;
    }
}