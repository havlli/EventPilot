package com.github.havlli.EventPilot.api.auth;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.net.URI;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("authenticate")
    public ResponseEntity<AuthResponse> authenticate(@RequestBody AuthRequest request) {
        AuthResponse authResponse = authService.authenticate(request);

        return ResponseEntity.ok()
                .header(HttpHeaders.AUTHORIZATION, authResponse.token())
                .body(authResponse);
    }

    @PostMapping("signup")
    public ResponseEntity<AuthResponse> signup(@RequestBody UserSignupRequest request) {
        AuthResponse authResponse = authService.signup(request);
        URI createdResourceLocation = URI.create("/api/users/" + authResponse.user().id());
        return ResponseEntity.created(createdResourceLocation)
                .header(HttpHeaders.AUTHORIZATION, authResponse.token())
                .body(authResponse);
    }
}
