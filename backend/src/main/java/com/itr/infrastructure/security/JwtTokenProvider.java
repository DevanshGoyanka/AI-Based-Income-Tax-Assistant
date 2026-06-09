package com.itr.infrastructure.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;

/**
 * JwtTokenProvider — creates and validates JWT tokens.
 * Access token: 15 minute expiry. Refresh token: 7 day expiry.
 */
@Component
public class JwtTokenProvider {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenProvider.class);
    
    private final SecretKey secretKey;
    private final long accessTokenExpirationMs;
    private final long refreshTokenExpirationMs;

    public JwtTokenProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.access-token-expiration-ms:900000}") long accessExpMs,
            @Value("${jwt.refresh-token-expiration-ms:604800000}") long refreshExpMs) {
        log.info("=== JwtTokenProvider init ===");
        log.info("JWT Secret (first 10 chars): {}", secret.substring(0, Math.min(10, secret.length())));
        log.info("JWT Access Exp: {}", accessExpMs);
        log.info("JWT Refresh Exp: {}", refreshExpMs);
        
        this.secretKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.accessTokenExpirationMs = accessExpMs;
        this.refreshTokenExpirationMs = refreshExpMs;
    }

    public String generateAccessToken(String userId, String role) {
        log.info("=== generateAccessToken called ===");
        log.info("userId: {}, role: {}", userId, role);
        try {
            String token = Jwts.builder()
                .subject(userId)
                .claim("role", role)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + accessTokenExpirationMs))
                .signWith(secretKey)
                .compact();
            log.info("Token generated, length: {}", token.length());
            return token;
        } catch (Exception e) {
            log.error("Error generating token: {}", e.getMessage(), e);
            return "";
        }
    }

    public String generateRefreshToken(String userId) {
        return Jwts.builder()
            .subject(userId)
            .claim("type", "refresh")
            .issuedAt(new Date())
            .expiration(new Date(System.currentTimeMillis() + refreshTokenExpirationMs))
            .signWith(secretKey)
            .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parser().verifyWith(secretKey).build().parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String getUserIdFromToken(String token) {
        return Jwts.parser()
            .verifyWith(secretKey)
            .build()
            .parseSignedClaims(token)
            .getPayload()
            .getSubject();
    }

    public List<SimpleGrantedAuthority> getAuthorities(String token) {
        try {
            String role = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload()
                .get("role", String.class);
            
            if (role == null || role.isEmpty()) {
                role = "USER"; // Default role
            }
            return List.of(new SimpleGrantedAuthority("ROLE_" + role));
        } catch (Exception e) {
            log.error("Error getting authorities from token: {}", e.getMessage());
            return List.of(new SimpleGrantedAuthority("ROLE_USER"));
        }
    }
}
