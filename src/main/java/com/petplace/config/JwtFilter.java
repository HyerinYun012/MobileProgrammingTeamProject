package com.petplace.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

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

            // Long.parseLong의 결과는 long(기본형)이므로 null일 수 없습니다. (경고 해결)
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
            log.warn("JWT Token expired: {}", e.getMessage());
            setErrorResponse(res, "토큰이 만료되었습니다. 다시 로그인해주세요.");
        } catch (JwtException | IllegalArgumentException e) {
            log.error("Invalid JWT Token: {}", e.getMessage());
            setErrorResponse(res, "유효하지 않은 인증 토큰입니다.");
        }
    }

    private void setErrorResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");

        Map<String, Object> errorDetails = new HashMap<>();
        errorDetails.put("success", false);
        errorDetails.put("message", message);
        errorDetails.put("data", null);

        // ObjectMapper를 사용하여 안전하게 JSON 직렬화
        String json = objectMapper.writeValueAsString(errorDetails);
        response.getWriter().write(json);
    }
}