package com.petplace.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtil {
    @Value("${jwt.secret}")
    private String secret;

    @Value("${jwt.expiration}")
    private long expiration;

    // 공통 키 생성 메서드
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 토큰 생성
     */
    public String generateToken(Long userId, String role) {
        return Jwts.builder()
                .subject(String.valueOf(userId))
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiration))
                .signWith(getSigningKey())
                .compact();
    }

    /**
     * [핵심 수정] 모든 클레임 추출 (단일 파싱)
     * JwtFilter에서 이 메서드를 호출하여 한 번만 파싱한 뒤 데이터를 재사용합니다.
     */
    public Claims extractAllClaims(String token) throws ExpiredJwtException, JwtException {
        return Jwts.parser()
                .verifyWith(getSigningKey()) // 최신 jjwt 버전(0.12+) 기준 문법
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    /**
     * 기존 메서드들 유지 (필요 시 개별 호출용)
     * 이제 내부적으로 extractAllClaims를 활용하여 중복 코드를 제거했습니다.
     */
    public String getRole(String token) {
        return extractAllClaims(token).get("role", String.class);
    }

    public Long getUserId(String token) {
        return Long.parseLong(extractAllClaims(token).getSubject());
    }

    /**
     * 유효성 검증
     * 이제 필터에서 직접 예외를 캐치하므로, 이 메서드의 사용 빈도는 줄어들지만
     * 단순 체크용으로 남겨둘 수 있습니다.
     */
    public boolean isValid(String token) {
        try {
            extractAllClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }
}