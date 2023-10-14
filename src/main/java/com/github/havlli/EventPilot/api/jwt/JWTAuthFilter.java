package com.github.havlli.EventPilot.api.jwt;

import com.github.havlli.EventPilot.api.auth.UserDetailsServiceImpl;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Optional;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;

@Component
public class JWTAuthFilter extends OncePerRequestFilter {

    private final UserDetailsServiceImpl userDetailService;
    private final JWTService jwtService;

    public JWTAuthFilter(UserDetailsServiceImpl userDetailService, JWTService jwtService) {
        this.userDetailService = userDetailService;
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        String authHeader = request.getHeader(AUTHORIZATION);

        if (isValidHeader(authHeader)) {
            String jwtToken = extractJwtToken(authHeader);
            validateSubjectThenSetAuthContext(request, jwtToken);
        }

        filterChain.doFilter(request, response);
    }

    private boolean isValidHeader(String authHeader) {
        return authHeader != null && authHeader.startsWith("Bearer");
    }

    private String extractJwtToken(String authHeader) {
        return authHeader.substring(7);
    }

    private void validateSubjectThenSetAuthContext(HttpServletRequest request, String jwtToken) {
        Optional<String> username = jwtService.extractUsername(jwtToken);

        if (username.isEmpty() || isAuthContextDefined()) {
            return;
        }

        UserDetails userDetails = userDetailService.loadUserByUsername(username.orElseThrow());

        if(jwtService.isTokenValid(jwtToken, userDetails)) {
            setAuthorizationContext(request, userDetails);
        }
    }

    private static boolean isAuthContextDefined() {
        return SecurityContextHolder.getContext().getAuthentication() != null;
    }

    private static UsernamePasswordAuthenticationToken createAuthenticationToken(UserDetails userDetails) {
        return new UsernamePasswordAuthenticationToken(
                userDetails,
                null,
                userDetails.getAuthorities()
        );
    }

    private static void setAuthorizationContext(HttpServletRequest request, UserDetails userDetails) {
        UsernamePasswordAuthenticationToken authToken = createAuthenticationToken(userDetails);
        authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
        SecurityContextHolder.getContext().setAuthentication(authToken);
    }
}
