package com.github.havlli.EventPilot.api.auth;

import com.github.havlli.EventPilot.api.jwt.JWTService;
import com.github.havlli.EventPilot.entity.user.User;
import com.github.havlli.EventPilot.entity.user.UserRole;
import com.github.havlli.EventPilot.entity.user.UserRoleService;
import com.github.havlli.EventPilot.entity.user.UserService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.util.Set;

@Service
public class AuthService {
    private final AuthenticationManager authManager;
    private final UserService userService;
    private final UserRoleService userRoleService;
    private final UserSignupRequestMapper userSignupRequestMapper;
    private final JWTService jwtService;

    public AuthService(
            AuthenticationManager authManager,
            UserService userService,
            UserRoleService userRoleService,
            UserSignupRequestMapper userSignupRequestMapper,
            JWTService jwtService
    ) {
        this.authManager = authManager;
        this.userService = userService;
        this.userRoleService = userRoleService;
        this.userSignupRequestMapper = userSignupRequestMapper;
        this.jwtService = jwtService;
    }

    public AuthResponse authenticate(AuthRequest request) {
        Authentication authentication = buildAuthTokenAndAuthenticate(request);
        UserDetailsImpl userDetails = getUserDetails(authentication);
        String jwtToken = generateJwtToken(userDetails);

        return AuthResponse.of(jwtToken, userDetails);
    }

    private Authentication buildAuthTokenAndAuthenticate(AuthRequest request) {
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(request.username(), request.password());
        return authManager.authenticate(authenticationToken);
    }

    private UserDetailsImpl getUserDetails(Authentication authentication) {
        return (UserDetailsImpl) authentication.getPrincipal();
    }

    private String generateJwtToken(UserDetailsImpl userDetails) {
        return jwtService.generateToken(userDetails);
    }

    public AuthResponse signup(UserSignupRequest request) {
        checkCredentialsAvailability(request);
        User user = mapUserFrom(request);
        String jwtToken = generateJwtToken(user);
        saveNewUser(user);

        return AuthResponse.of(jwtToken, user);
    }

    private void checkCredentialsAvailability(UserSignupRequest request) {
        userService.checkUsernameAvailability(request.username());
        userService.checkEmailAvailability(request.email());
    }

    private User mapUserFrom(UserSignupRequest request) {
        UserRole userRole = userRoleService.getDefaultUserRole();
        return userSignupRequestMapper.mapUser(request, Set.of(userRole));
    }

    private String generateJwtToken(User user) {
        return jwtService.generateToken(user);
    }

    private void saveNewUser(User user) {
        userService.saveUser(user);
    }
}
