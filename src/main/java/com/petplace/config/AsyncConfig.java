package com.petplace.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;

@Configuration
@EnableAsync
public class AsyncConfig {

    /**
     * 비동기 작업을 처리할 스레드 풀(TaskExecutor) 설정
     * 💡 운영 환경에 맞게 적절히 튜닝하여 사용하세요.
     */
    @Bean(name = "taskExecutor")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();

        executor.setCorePoolSize(5);        // 기본적으로 유지할 스레드 수
        executor.setMaxPoolSize(10);        // 최대 스레드 수
        executor.setQueueCapacity(500);     // 대기열 큐 사이즈 (큐가 차면 스레드를 추가로 생성)
        executor.setThreadNamePrefix("Async-"); // 스레드 이름 접두사 (디버깅 용이)
        executor.initialize();

        return executor;
    }
}