package com.petplace.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtUtil jwtUtil;

    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // CSRF 보호 비활성화 (API 서버이므로)
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS) // 세션 미사용 (JWT 사용)
                )
                .authorizeHttpRequests(auth -> auth
                        // 1. 누구나 접근 가능한 경로 (Permit All)
                        .requestMatchers("/", "/favicon.ico", "/error").permitAll() // 메인, 아이콘, 에러 페이지 허용
                        .requestMatchers("/api/auth/**", "/api/search/**").permitAll() // 인증 및 검색 관련 API 허용
                        .requestMatchers(HttpMethod.GET, "/api/restaurants/**").permitAll() // 맛집 조회는 누구나 가능

                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()

                        // 3. 권한별 접근 제한 (Role Based)
                        .requestMatchers("/api/admin/**").hasRole("ADMIN") // 관리자 전용
                        .requestMatchers(HttpMethod.POST, "/api/restaurants/**").hasAnyRole("OWNER", "ADMIN") // 등록
                        .requestMatchers(HttpMethod.PUT, "/api/restaurants/**").hasAnyRole("OWNER", "ADMIN")  // 수정
                        .requestMatchers(HttpMethod.DELETE, "/api/restaurants/**").hasAnyRole("OWNER", "ADMIN") // 삭제

                        // 4. 그 외 모든 요청은 인증 필요
                        .anyRequest().authenticated()
                )
                // JWT 필터 추가
                .addFilterBefore(new JwtFilter(jwtUtil, objectMapper()), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}