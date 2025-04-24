package com.RoadCloudVisualizationSystem.utils;

import io.jsonwebtoken.*;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.UUID;

public class JwtUtil {
    public static final long JWT_TTL = 60 * 60 * 1000L * 2; // 2个小时
    private static final String JWT_KEY = "SDFGjhdsfalshdfHFdsjkdsfds121232131afasdfac";

    public static String createJWT(String subject) {
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .subject(subject)
                .issuer("sg")
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + JWT_TTL))
                .signWith(generalKey(), Jwts.SIG.HS256)
                .compact();
    }

    public static SecretKey generalKey() {
        return new SecretKeySpec(JWT_KEY.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
    }

    public static Claims parseJWT(String jwt) throws JwtException {
        return Jwts.parser()
                .verifyWith(generalKey())
                .build()
                .parseSignedClaims(jwt)
                .getPayload();
    }
}