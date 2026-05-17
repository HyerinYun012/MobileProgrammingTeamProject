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
            // 💡 [메서드 추출 완성] 복잡한 토큰 파싱 및 토큰 생성 로직을 하단 전용 메서드로 위임합니다.
            UsernamePasswordAuthenticationToken auth = createAuthentication(token);

            if (auth != null) {
                SecurityContextHolder.getContext().setAuthentication(auth);
            }

            chain.doFilter(req, res);

        } catch (ExpiredJwtException e) {
            log.warn("JWT Token expired", e);
            resolver.resolveException(req, res, null, e);
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid JWT Token", e);
            resolver.resolveException(req, res, null, e);
        }
    }

    /**
     *  [추출된 메서드] JWT 토큰을 파싱하여 Spring Security 맞춤형 인증 토큰(auth)을 빌드합니다.
     */
    private UsernamePasswordAuthenticationToken createAuthentication(String token) {
        Claims claims = jwtUtil.extractAllClaims(token);

        long userId = Long.parseLong(claims.getSubject());
        String role = claims.get("role", String.class);

        if (role == null) {
            return null;
        }

        String authority = role.startsWith("ROLE_") ? role : "ROLE_" + role;

        return new UsernamePasswordAuthenticationToken(
                userId,
                "",
                List.of(new SimpleGrantedAuthority(authority))
        );
    }
}