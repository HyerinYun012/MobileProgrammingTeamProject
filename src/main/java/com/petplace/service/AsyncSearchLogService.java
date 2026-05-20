package com.petplace.service;

import com.petplace.entity.SearchLog;
import com.petplace.repository.SearchLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AsyncSearchLogService {

    private final SearchLogRepository searchLogRepository;

    /**
     * 🚀 [비동기 처리] 로그 저장 작업을 별도 스레드에서 처리하여
     * 메인 검색 로직의 응답 속도에 영향을 주지 않도록 합니다.
     */
    @Async
    public void saveLogAsync(String keyword) {
        try {
            searchLogRepository.save(new SearchLog(keyword));
            log.debug("Async search log saved: {}", keyword);
        } catch (Exception e) {
            // 비동기 처리 중 예외 발생 시 메인 트랜잭션과 무관하게 에러만 로깅
            log.error("Failed to save search log asynchronously", e);
        }
    }
}