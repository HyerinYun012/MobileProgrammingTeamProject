package com.petplace.config;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
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

    // 서버 구동 시점에 한 번만 만들어 안전하게 재사용할 키 객체
    private SecretKey signingKey;

    /**
     * [핵심 방어선] 의존성 주입이 완료된 후, 자동으로 실행되는 초기화 메서드
     */
    @PostConstruct
    public void init() {
        byte[] keyBytes = secret.getBytes(StandardCharsets.UTF_8);

        // 만약 yml 파일의 키 길이가 256비트(32바이트) 미만이라면 서버 구동 단계에서 조기 경보를 울립니다.
        if (keyBytes.length < 32) {
            throw new IllegalArgumentException(
                    "❌ [보안 오류] JWT 서명 비밀키(jwt.secret)의 길이는 최소 32바이트(영문/숫자 기준 32자) 이상이어야 합니다! " +
                            "현재 길이: " + keyBytes.length + "바이트"
            );
        }

        // 안전함이 검증된 키를 필드에 할당하여 싱글톤 형태로 관리합니다.
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * 초기화된 안전한 키를 반환합니다.
     */
    private SecretKey getSigningKey() {
        return this.signingKey;
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
     * 모든 클레임 추출 (단일 파싱)
     * 💡 중복된 throws 선언을 정리하여 상위 예외인 JwtException만 깔끔하게 남겼습니다.
     */
    public Claims extractAllClaims(String token) throws JwtException {
        return Jwts.parser()
                .verifyWith(getSigningKey()) // 최신 jjwt 버전(0.12+) 기준 문법
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}