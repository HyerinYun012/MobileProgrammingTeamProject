package com.petplace.config;

import org.springframework.beans.factory.annotation.Qualifier;
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
import org.springframework.web.servlet.HandlerExceptionResolver;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final JwtUtil jwtUtil;
    private final HandlerExceptionResolver handlerExceptionResolver;

    public SecurityConfig(JwtUtil jwtUtil,
                          @Qualifier("handlerExceptionResolver") HandlerExceptionResolver handlerExceptionResolver) {
        this.jwtUtil = jwtUtil;
        this.handlerExceptionResolver = handlerExceptionResolver;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable) // API 서버이므로 CSRF 비활성화
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS) // 세션 미사용 (JWT 필수 체계)
                )
                .authorizeHttpRequests(auth -> auth
                        // 1. 최소한의 시스템 허용 경로 (로그인 및 사장님/고객 회원가입 창구만 오픈)
                        .requestMatchers("/", "/favicon.ico", "/error").permitAll()
                        .requestMatchers("/api/auth/**").permitAll() // 로그인, 회원가입, 중복체크 API만 비인증 접근 가능

                        // 2. 프론트엔드/백엔드 협업용 API 문서 명세서 인프라 허용
                        .requestMatchers(
                                "/v3/api-docs/**",
                                "/swagger-ui/**",
                                "/swagger-ui.html",
                                "/swagger-resources/**",
                                "/webjars/**"
                        ).permitAll()

                        //비로그인 유저도 식당 조회(목록, 상세, 검색 등) 가능
                        .requestMatchers(HttpMethod.GET, "/api/restaurants/**").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/search", "/api/search/popular").permitAll()

                        // 권한별 접근 제한 (Role Based)
                        .requestMatchers("/api/admin/**").hasRole("ADMIN") // 관리자 전용 기능역역
                        .requestMatchers(HttpMethod.POST, "/api/restaurants/**").hasAnyRole("OWNER", "ADMIN") // 가게 등록
                        .requestMatchers(HttpMethod.PUT, "/api/restaurants/**").hasAnyRole("OWNER", "ADMIN")  // 가게 수정
                        .requestMatchers(HttpMethod.DELETE, "/api/restaurants/**").hasAnyRole("OWNER", "ADMIN") // 가게 삭제

                        // 4. 나머지 모든 API 요청(맛집 단순 GET 조회, 검색, 커뮤니티, 리뷰 전체 포함)은 무조건 인증된 회원만 진입 허용
                        .anyRequest().authenticated()
                )
                // 필터 체인 앞단에 JWT 검증 필터를 배치하여 토큰이 없거나 올바르지 않으면 컨트롤러 도달 전에 쳐냅니다.
                .addFilterBefore(new JwtFilter(jwtUtil, handlerExceptionResolver), UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public BCryptPasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }
}