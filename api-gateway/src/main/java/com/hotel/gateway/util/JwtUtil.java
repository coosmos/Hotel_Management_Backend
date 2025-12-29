package com.hotel.gateway.util;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

@Component
public class JwtUtil {

    @Value("${jwt.secret}")
    private String secretKey;

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public String extractEmail(String token) {
        return extractClaim(token, claims -> claims.get("email", String.class));
    }
    public Long extractUserId(String token) {
        return extractClaim(token, claims -> claims.get("userId", Long.class));
    }
    public String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }
    public Long extractHotelId(String token) {
        return extractClaim(token, claims -> claims.get("hotelId", Long.class));
    }
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSignInKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    // Get the signing key
    private SecretKey getSignInKey() {
        byte[] keyBytes = Decoders.BASE64.decode(secretKey);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    // Check if token is expired
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    // Validate token - checks both signature and expiration
    public Boolean validateToken(String token) {
        try {
            // This will validate signature and structure
            Claims claims = extractAllClaims(token);
            // Also check expiration
            return !claims.getExpiration().before(new Date());
        } catch (io.jsonwebtoken.ExpiredJwtException e) {
            return false; // Token expired
        } catch (io.jsonwebtoken.security.SignatureException e) {
            return false; // Invalid signature
        } catch (io.jsonwebtoken.MalformedJwtException e) {
            return false; // Malformed token
        } catch (Exception e) {
            return false; // Any other error
        }
    }
}