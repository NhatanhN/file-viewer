package net.nhatanhn.services;

import java.nio.charset.StandardCharsets;
import java.util.Date;

import javax.crypto.SecretKey;

import org.mindrot.jbcrypt.BCrypt;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

public class Auth {

    private static final String secret = "example secret (should be very long)123123123";
    private static final SecretKey key =  Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));

    public static String generateToken(long userId) {
        return Jwts.builder()
                .subject(Long.toString(userId))
                .expiration(new Date(System.currentTimeMillis() + 36000000)) // 10 hours
                .signWith(key)
                .compact();
    }

    public static long verifyToken(String token) {
        try {
            String id = Jwts.parser().verifyWith(key).build().parseSignedClaims(token).getPayload().getSubject();
            return Long.parseLong(id);
        } catch (JwtException e) {
            return -1l;
        }
    }

    public static String hashPassword(String password) {
        return BCrypt.hashpw(password, BCrypt.gensalt());
    }

    public static boolean verifyPassword(String password, String hashedPassword) {
        return BCrypt.checkpw(password, hashedPassword);
    }
}
