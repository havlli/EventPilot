package com.github.havlli.EventPilot.api.jwt;

import com.github.havlli.EventPilot.api.auth.UserDetailsImpl;
import com.github.havlli.EventPilot.entity.user.User;
import io.jsonwebtoken.*;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;

@Component
public class JWTService {
    private static final Logger LOG = LoggerFactory.getLogger(JWTService.class);
    private String jwtSecret = "=======================================Secret==========================";

    public Optional<String> extractUsername(String token) {
        try {
            String subject = extractClaim(token, Claims::getSubject);
            return subject == null ? Optional.empty() : Optional.of(subject);
        } catch (JwtException e) {
            LOG.error("extractUsername - {} - {}", e.getClass(), e.getMessage());
            return Optional.empty();
        }
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public boolean hasClaim(String token, String claimName) {
        final Claims claims = extractAllClaims(token);
        return claims.get(claimName) != null;
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token)
                .before(new Date(System.currentTimeMillis()));
    }

    public Boolean isTokenValid(String token, UserDetails userDetails) {
        final String userName = extractUsername(token).orElseThrow();
        return (userName.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }

    private Claims extractAllClaims(String token) {
            return Jwts.parserBuilder()
                    .setSigningKey(getKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();
    }

    private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        return createToken(claims, userDetails);
    }

    public String generateToken(User user) {
        Map<String, Object> claims = new HashMap<>();
        UserDetailsImpl userDetails = UserDetailsImpl.of(user);
        return createToken(claims, userDetails);
    }

    public String generateToken(UserDetails userDetails, Map<String, Object> claims) {
        return createToken(claims, userDetails);
    }

    private String createToken(Map<String, Object> claims, UserDetails userDetails) {
        return Jwts.builder()
                .setClaims(claims)
                .setSubject(userDetails.getUsername())
                .claim("authorities", userDetails.getAuthorities())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10))
                .signWith(getKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Key getKey() {
        return Keys.hmacShaKeyFor(Decoders.BASE64.decode(jwtSecret));
    }
}
