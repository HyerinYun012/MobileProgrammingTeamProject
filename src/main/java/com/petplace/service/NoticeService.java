package com.petplace.service;

import com.petplace.entity.Notice;
import com.petplace.entity.Restaurant;
import com.petplace.exception.BusinessException;
import com.petplace.repository.NoticeRepository;
import com.petplace.repository.RestaurantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;
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
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new BusinessException("가게를 찾을 수 없습니다."));

        if (restaurant.getOwner() == null || !Objects.equals(restaurant.getOwner().getId(), ownerId)) {
            throw new BusinessException("공지사항 작성 권한이 없습니다.");
        }

        // 🌟 [수정] 외부 Setter 주입 방식 대신 캡슐화된 정적 팩토리 메서드를 호출하여 완결성 있는 객체를 만듭니다.
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
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new BusinessException("공지사항을 찾을 수 없습니다."));

        if (notice.getRestaurant() == null || notice.getRestaurant().getOwner() == null ||
                !Objects.equals(notice.getRestaurant().getOwner().getId(), ownerId)) {
            throw new BusinessException("공지사항 수정 권한이 없습니다.");
        }

        // 🌟 [수정] 뱀 모양(Snake Case) 필드를 지우고 카멜 케이스 getter로 파일 변경 감지 수행
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

        // 🌟 [수정] 나열식 Setter 구조를 제거하고 단 한 줄의 도메인 비즈니스 메서드로 수정을 위임합니다. (Dirty Checking)
        notice.updateNotice(title, content, newThumbUrl, newDescImgUrl);
    }

    /**
     * 공지사항 삭제 (공지를 올린 사장님만 가능 + S3 파일 연쇄 삭제)
     */
    @Transactional
    public void deleteNotice(Long noticeId, Long ownerId) {
        Notice notice = noticeRepository.findById(noticeId)
                .orElseThrow(() -> new BusinessException("공지사항을 찾을 수 없습니다."));

        if (notice.getRestaurant() == null || notice.getRestaurant().getOwner() == null ||
                !Objects.equals(notice.getRestaurant().getOwner().getId(), ownerId)) {
            throw new BusinessException("공지사항 삭제 권한이 없습니다.");
        }

        // 🌟 [수정] 삭제 완료(Commit) 후 물리 파일 파괴 로직 역시 수정된 카멜 케이스 메서드를 바라보도록 대응합니다.
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
     * 특정 식당의 공지사항 목록 최신순 조회
     */
    public List<Notice> getNoticesByRestaurant(Long restaurantId) {
        restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new BusinessException("가게를 찾을 수 없습니다."));

        return noticeRepository.findByRestaurant_IdOrderByCreatedAtDesc(restaurantId);
    }
}