package com.petplace.service;

import com.petplace.dto.response.NoticeResponse;
import com.petplace.entity.Notice;
import com.petplace.entity.Restaurant;
import com.petplace.exception.BusinessException;
import com.petplace.exception.ErrorCode;
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
     * 공지사항 작성
     */
    @Transactional
    public void createNotice(Long restaurantId, Long ownerId, String title, String content, String thumbUrl, String descImgUrl) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESTAURANT_NOT_FOUND));

        if (restaurant.getOwner() == null || !Objects.equals(restaurant.getOwner().getId(), ownerId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

        // 💡 [추가] 사장님 계정 승인 여부 검증
        if (!restaurant.getOwner().isVerified()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_OWNER);
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
     * 공지사항 수정
     */
    @Transactional
    public void updateNotice(Long noticeId, Long ownerId, String title, String content, String newThumbUrl, String newDescImgUrl) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTICE_NOT_FOUND));

        if (notice.getRestaurant() == null || notice.getRestaurant().getOwner() == null ||
                !Objects.equals(notice.getRestaurant().getOwner().getId(), ownerId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

        // 💡 [추가] 사장님 계정 승인 여부 검증
        if (!notice.getRestaurant().getOwner().isVerified()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_OWNER);
        }

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
     * 공지사항 삭제
     */
    @Transactional
    public void deleteNotice(Long noticeId, Long ownerId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOTICE_NOT_FOUND));

        if (notice.getRestaurant() == null || notice.getRestaurant().getOwner() == null ||
                !Objects.equals(notice.getRestaurant().getOwner().getId(), ownerId)) {
            throw new BusinessException(ErrorCode.NO_PERMISSION);
        }

        // 💡 [추가] 사장님 계정 승인 여부 검증
        if (!notice.getRestaurant().getOwner().isVerified()) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED_OWNER);
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
        restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new BusinessException(ErrorCode.RESTAURANT_NOT_FOUND));

        return noticeRepository.findByRestaurantIdOrderByCreatedAtDesc(restaurantId, pageable)
                .map(NoticeResponse::from);
    }
}