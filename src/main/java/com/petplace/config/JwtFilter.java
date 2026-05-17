package com.petplace.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.servlet.HandlerExceptionResolver;

import java.io.IOException;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    // 스프링의 예외 위임 해결사 주입
    private final HandlerExceptionResolver resolver;

    @Override
    protected void doFilterInternal(
            @jakarta.annotation.Nonnull HttpServletRequest req,
            @jakarta.annotation.Nonnull HttpServletResponse res,
            @jakarta.annotation.Nonnull FilterChain chain) throws ServletException, IOException {

        String header = req.getHeader("Authorization");

        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(req, res);
            return;
        }

        String token = header.substring(7);

        try {
            Claims claims = jwtUtil.extractAllClaims(token);

            long userId = Long.parseLong(claims.getSubject());
            String role = claims.get("role", String.class);

            if (role != null) {
                String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role;

                UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                        userId,
                        null,
                        List.of(new SimpleGrantedAuthority(authority))
                );

                SecurityContextHolder.getContext().setAuthentication(auth);
            }

            chain.doFilter(req, res);

        } catch (ExpiredJwtException e) {
            // 💡 e.getMessage() 대신 예외 객체 e를 통째로 넘겨 스택 트레이스 확보 (warn 레벨)
            log.warn("JWT Token expired", e);
            // 톰캣 필터 시점에서 터진 예외를 ControllerAdvice로 안전하게 토스
            resolver.resolveException(req, res, null, e);
        } catch (JwtException | IllegalArgumentException e) {
            // 💡 위변조 및 잘못된 토큰 유입 시 상세 경로를 로그에 완벽히 기록 (error 레벨)
            log.error("Invalid JWT Token", e);
            resolver.resolveException(req, res, null, e);
        }
    }
}