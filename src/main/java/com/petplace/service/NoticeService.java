package com.petplace.service;

import com.petplace.dto.response.NoticeResponse;
import com.petplace.entity.Notice;
import com.petplace.entity.Restaurant;
import com.petplace.exception.BusinessException;
import com.petplace.exception.ErrorCode; // 💡 ErrorCode import 추가
import com.petplace.repository.NoticeRepository;
import com.petplace.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.Objects;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final RestaurantRepository restaurantRepository;
    private final FileService fileService;

    /**
     * 공지사항 작성 (해당 가게 사장님만 가능)
     */
    @Transactional
    public void createNotice(Long restaurantId, Long ownerId, String title, String content, String thumbUrl, String descImgUrl) {
        // 💡 ErrorCode 적용
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESTAURANT_NOT_FOUND));

        if (restaurant.getOwner() == null || !Objects.equals(restaurant.getOwner().getId(), ownerId)) {
            // 💡 ErrorCode 적용
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

        Notice notice = Notice.createNotice(
                restaurant,
                title,
                content,
                thumbUrl,
                descImgUrl
        );

        noticeRepository.save(notice);
    }

    /**
     * 공지사항 수정 (공지를 올린 사장님만 가능 + S3 파일 롤백 방어 적용)
     */
    @Transactional
    public void updateNotice(Long noticeId, Long ownerId, String title, String content, String newThumbUrl, String newDescImgUrl) {
        // 💡 ErrorCode 적용
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTICE_NOT_FOUND));

        if (notice.getRestaurant() == null || notice.getRestaurant().getOwner() == null ||
                !Objects.equals(notice.getRestaurant().getOwner().getId(), ownerId)) {
            // 💡 ErrorCode 적용
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

        // 1. 썸네일 이미지가 변경된 경우
        if (notice.getThumbnailUrl() != null && !notice.getThumbnailUrl().isEmpty() &&
                !Objects.equals(notice.getThumbnailUrl(), newThumbUrl)) {

            String oldThumb = notice.getThumbnailUrl();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    fileService.deleteFile(oldThumb);
                }
            });
        }

        // 2. 본문 상세 이미지가 변경된 경우
        if (notice.getDescriptionImageUrl() != null && !notice.getDescriptionImageUrl().isEmpty() &&
                !Objects.equals(notice.getDescriptionImageUrl(), newDescImgUrl)) {

            String oldDescImg = notice.getDescriptionImageUrl();
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    fileService.deleteFile(oldDescImg);
                }
            });
        }

        notice.updateNotice(title, content, newThumbUrl, newDescImgUrl);
    }

    /**
     * 공지사항 삭제 (공지를 올린 사장님만 가능 + S3 파일 연쇄 삭제)
     */
    @Transactional
    public void deleteNotice(Long noticeId, Long ownerId) {
        // 💡 ErrorCode 적용
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTICE_NOT_FOUND));

        if (notice.getRestaurant() == null || notice.getRestaurant().getOwner() == null ||
                !Objects.equals(notice.getRestaurant().getOwner().getId(), ownerId)) {
            // 💡 ErrorCode 적용
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                if (notice.getThumbnailUrl() != null && !notice.getThumbnailUrl().isEmpty()) {
                    fileService.deleteFile(notice.getThumbnailUrl());
                }
                if (notice.getDescriptionImageUrl() != null && !notice.getDescriptionImageUrl().isEmpty()) {
                    fileService.deleteFile(notice.getDescriptionImageUrl());
                }
            }
        });

        noticeRepository.delete(notice);
    }

    /**
     * 특정 식당의 공지사항 목록 최신순 페이징 조회
     */
    public Page<NoticeResponse> getNoticesByRestaurant(Long restaurantId, Pageable pageable) {
        // 1. 식당 존재 여부 확인
        restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESTAURANT_NOT_FOUND));

        return noticeRepository.findByRestaurantIdOrderByCreatedAtDesc(restaurantId, pageable)
                .map(NoticeResponse::from);
    }
}