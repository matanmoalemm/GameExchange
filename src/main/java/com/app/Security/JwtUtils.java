package com.app.Security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtUtils {

    // Usually stored in application.properties
    @Value("${app.jwtSecret}")
    private String jwtSecret;

    @Value("${app.jwtExpirationMs}")
    private int jwtExpirationMs;

    /**
     * Converts the secret string into a secure key for signing the JWT.
     */
    private SecretKey getSigningKey() {
        byte[] keyBytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    /**
     * Generates a JWT token for a specific user after a successful login.
     */
    public String generateToken(UserPrincipal principal) {
        return Jwts.builder()
                .subject(String.valueOf(principal.getId())) // Set the user ID as subject
                .claim("email", principal.getEmail())       // Custom claim for the email
                .issuedAt(new Date())                       // Current time
                .expiration(new Date((new Date()).getTime() + jwtExpirationMs)) // Expiry time
                .signWith(getSigningKey())                  // Sign with our secret key
                .compact();                                 // Finalize the string
    }

    /**
     * Extracts the User ID from the JWT payload.
     */
    public Long getUserIdFromToken(String token) {
        String subject = Jwts.parser()
                .verifyWith(getSigningKey()) // Verify the signature first
                .build()
                .parseSignedClaims(token)    // Parse the token claims
                .getPayload()
                .getSubject();

        return Long.parseLong(subject);
    }

    /**
     * Validates if the token is properly formatted, correctly signed, and not expired.
     */
    public boolean validateToken(String token) {
        try {
            Jwts.parser()
                    .verifyWith(getSigningKey())
                    .build()
                    .parseSignedClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            // Log the error (ExpiredJwtException, MalformedJwtException, etc.)
            return false;
        }
    }
}