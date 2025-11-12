package com.example.demo.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import java.security.Key;
import java.util.Date;
import java.nio.charset.StandardCharsets;

import org.springframework.stereotype.Component;
import org.springframework.beans.factory.annotation.Value;
//import javax.annotation.PostConstruct;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secret;

    /**
     * expiration time in milliseconds (set via property jwt.expiration)
     * default fallback will be 3600000 (1 hour) if not set.
     */
    @Value("${jwt.expiration:3600000}")
    private long expiration;

    private Key key;

    @jakarta.annotation.PostConstruct
    public void init() {
        if (secret == null || secret.length() < 32) {
            // It's crucial that the secret is sufficiently long for HS256 (>= 32 bytes recommended)
            throw new IllegalStateException("jwt.secret is not set or too short. Provide a strong secret (32+ chars) via config.");
        }
        key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
    }

    // Generate token with optional role claim
    public String generateToken(String username, String role) {
        JwtBuilder builder = Jwts.builder()
                .setSubject(username)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + expiration));

        if (role != null) {
            builder.claim("role", role);
        }

        return builder.signWith(key, SignatureAlgorithm.HS256).compact();
    }

    // Extract username (subject)
    public String extractUsername(String token) {
        return extractClaims(token).getSubject();
    }

    // Extract role (if present)
    public String extractRole(String token) {
        return extractClaims(token).get("role", String.class);
    }

    // Validate token: checks subject matches and not expired (also catches malformed/expired tokens)
    public boolean validateToken(String token, String username) {
        try {
            final String tokenUsername = extractUsername(token);
            return (username != null && username.equals(tokenUsername) && !isTokenExpired(token));
        } catch (JwtException | IllegalArgumentException e) {
            // token invalid / expired / tampered
            return false;
        }
    }

    // Check expiration
    private boolean isTokenExpired(String token) {
        Date exp = extractClaims(token).getExpiration();
        return exp == null || exp.before(new Date());
    }

    // Parse & return Claims (will throw on invalid token)
    private Claims extractClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    // create pre-auth token (expiry e.g., 5 minutes)
    public String generatePreAuthToken(String username) {
        long preAuthExpiry = 5 * 60 * 1000; // 5 minutes
        return Jwts.builder()
                .setSubject(username)
                .claim("preAuth", true)
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + preAuthExpiry))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validatePreAuthToken(String token) {
        try {
            Claims claims = extractClaims(token);
            Boolean preAuth = claims.get("preAuth", Boolean.class);
            return Boolean.TRUE.equals(preAuth) && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

}