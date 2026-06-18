package com.studyroom.common.util;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Component;
import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {
    private static final SecretKey KEY = Keys.hmacShaKeyFor("study-room-secret-key-256-bits-long!!".getBytes());
    private static final long EXPIRE = 24 * 60 * 60 * 1000L;

    public String generate(Long userId, String role) {
        return Jwts.builder().subject(String.valueOf(userId)).claim("role", role)
            .issuedAt(new Date()).expiration(new Date(System.currentTimeMillis() + EXPIRE)).signWith(KEY).compact();
    }
    public Claims parse(String token) { return Jwts.parser().verifyWith(KEY).build().parseSignedClaims(token).getPayload(); }
    public Long getUserId(String token) { return Long.parseLong(parse(token).getSubject()); }
    public String getRole(String token) { return parse(token).get("role", String.class); }
    public boolean isTokenValid(String token) {
        try { parse(token); return true; } catch (Exception e) { return false; }
    }
}
