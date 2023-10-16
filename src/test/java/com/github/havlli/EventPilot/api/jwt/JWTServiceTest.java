package com.github.havlli.EventPilot.api.jwt;

import com.github.havlli.EventPilot.api.auth.UserDetailsImpl;
import com.github.havlli.EventPilot.entity.user.User;
import com.github.havlli.EventPilot.entity.user.UserRole;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Encoders;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockitoAnnotations;

import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.spy;

class JWTServiceTest {

    private AutoCloseable autoCloseable;
    private JWTService underTest;
    private String jwtSecret;

    @BeforeEach
    void setUp() {
        autoCloseable = MockitoAnnotations.openMocks(this);
        jwtSecret = generateKey();
        underTest = new JWTService(jwtSecret);
    }

    private String generateKey() {
        byte[] encodedSecret = Keys.secretKeyFor(SignatureAlgorithm.HS256).getEncoded();
        return Encoders.BASE64.encode(encodedSecret);
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    void hasClaim_ReturnsTrue_WhenClaimIsPresent() {
        // Arrange
        UserRole userRole = new UserRole(UserRole.Role.USER);
        User user = new User(null, "username", "email", "password", Set.of(userRole));
        Map<String, Object> claims = Map.of("present-claim", "present-value");

        String jwtToken = underTest.generateToken(user, claims);
        System.out.println(jwtToken);

        // Act
        boolean result = underTest.hasClaim(jwtToken, "present-claim");

        // Assert
        assertThat(result).isTrue();
    }

    @Test
    void hasClaim_ReturnsFalse_WhenClaimNotPresent() {
        // Arrange
        UserRole userRole = new UserRole(UserRole.Role.USER);
        User user = new User(null, "username", "email", "password", Set.of(userRole));

        String jwtToken = underTest.generateToken(user);

        // Act
        boolean result = underTest.hasClaim(jwtToken, "not-existing-claim");

        // Assert
        assertThat(result).isFalse();
    }

    @Test
    void hasClaim_ThrowsSignatureException_WhenTokenSignatureIsInvalid() {
        // Arrange
        UserRole userRole = new UserRole(UserRole.Role.USER);
        User user = new User(null, "username", "email", "password", Set.of(userRole));

        String jwtSecret = generateKey();
        JWTService underTestSpy = spy(new JWTService(jwtSecret));
        String jwtToken = underTestSpy.generateToken(user);
        String invalidSignatureToken = jwtToken.substring(0, jwtToken.length() - 5);

        // Act & Assert
        assertThatThrownBy(() -> underTestSpy.hasClaim(invalidSignatureToken, "not-existing-claim"))
                .isInstanceOf(SignatureException.class);
    }

    @Test
    void hasClaim_ThrowsMalformedException_WhenTokenHeaderIsInvalid() {
        // Arrange
        UserRole userRole = new UserRole(UserRole.Role.USER);
        User user = new User(null, "username", "email", "password", Set.of(userRole));

        String jwtSecret = generateKey();
        JWTService underTestSpy = spy(new JWTService(jwtSecret));
        String jwtToken = underTestSpy.generateToken(user);
        String invalidHeaderToken = jwtToken.substring(5);

        // Act & Assert
        assertThatThrownBy(() -> underTestSpy.hasClaim(invalidHeaderToken, "not-existing-claim"))
                .isInstanceOf(MalformedJwtException.class);
    }

    @Test
    void hasClaim_ThrowsMalformedException_WhenTokenPayloadIsInvalid() {
        // Arrange
        UserRole userRole = new UserRole(UserRole.Role.USER);
        User user = new User(null, "username", "email", "password", Set.of(userRole));

        String jwtSecret = generateKey();
        JWTService underTestSpy = spy(new JWTService(jwtSecret));
        String jwtToken = underTestSpy.generateToken(user);

        String[] splitToken = jwtToken.split("\\.");
        splitToken[1] = "eyJzdWIiOiJ1c2VybmFtZSIsImF1dGhvcml0aWVzIjpboiJhdXRob3JpdHkiOiJVU0VSIn1dLCJpYXQiOjE2OTczOTIxMzMsImV4cCI6MTY5NzQyODEzM30";
        String invalidPayloadToken = String.join(".", splitToken);

        // Act & Assert
        assertThatThrownBy(() -> underTestSpy.hasClaim(invalidPayloadToken, "not-existing-claim"))
                .isInstanceOf(MalformedJwtException.class);
    }

    @Test
    void isTokenValid_ReturnsTrue_WhenTokenIsValidAndMatchesUserDetails() {
        // Arrange
        UserRole userRole = new UserRole(UserRole.Role.USER);
        User user = new User(null, "username", "email", "password", Set.of(userRole));

        String jwtSecret = generateKey();
        JWTService underTestSpy = spy(new JWTService(jwtSecret));
        String validToken = underTestSpy.generateToken(user);

        UserDetailsImpl userDetails = UserDetailsImpl.of(user);

        // Act
        Boolean actual = underTestSpy.isTokenValid(validToken, userDetails);

        // Assert
        assertThat(actual).isTrue();
    }

    @Test
    void isTokenValid_ReturnsFalse_WhenTokenIsValidAndNotMatchesUsername() {
        // Arrange
        UserRole userRole = new UserRole(UserRole.Role.USER);
        User user = new User(null, "username", "email", "password", Set.of(userRole));

        String jwtSecret = generateKey();
        JWTService underTestSpy = spy(new JWTService(jwtSecret));
        String validToken = underTestSpy.generateToken(user);

        User differentUser = new User(null, "username1", "email", "password", Set.of(userRole));
        UserDetailsImpl userDetails = UserDetailsImpl.of(differentUser);

        // Act
        Boolean actual = underTestSpy.isTokenValid(validToken, userDetails);

        // Assert
        assertThat(actual).isFalse();
    }

    @Test
    void isTokenValid_ThrowsJWTException_WhenTokenIsInvalidAndMatchesUsername() {
        // Arrange
        UserRole userRole = new UserRole(UserRole.Role.USER);
        User user = new User(null, "username", "email", "password", Set.of(userRole));

        String jwtSecret = generateKey();
        JWTService underTestSpy = spy(new JWTService(jwtSecret));
        String validToken = underTestSpy.generateToken(user);
        String[] splitToken = validToken.split("\\.");
        splitToken[1] = "eyJzdWIiOiJ1c2VybmFtZSIsImF1dGhvcml0aWVzIjpboiJhdXRob3JpdHkiOiJVU0VSIn1dLCJpYXQiOjE2OTczOTIxMzMsImV4cCI6MTY5NzQyODEzM30";
        String invalidPayloadToken = String.join(".", splitToken);

        UserDetailsImpl userDetails = UserDetailsImpl.of(user);

        // Act & Assert
        assertThatThrownBy(() -> underTestSpy.isTokenValid(invalidPayloadToken, userDetails))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("Invalid Token");
    }

    @Test
    void extractUsername_ReturnsUsernameOptional_WhenTokenIsValid() {
        // Arrange
        UserRole userRole = new UserRole(UserRole.Role.USER);
        User user = new User(1L, "username", "email", "password", Set.of(userRole));

        String token = underTest.generateToken(user);

        // Act
        Optional<String> actual = underTest.extractUsername(token);

        // Assert
        assertThat(actual).isPresent();
        assertThat(actual).hasValue("username");
    }

    @Test
    void extractUsername_ReturnsEmptyOptional_WhenTokenIsValidAndUsernameIsNotPresent() {
        // Arrange
        UserRole userRole = new UserRole(UserRole.Role.USER);
        User user = new User(1L, "username", "email", "password", Set.of(userRole));

        String token = underTest.generateToken(user);
        String[] tokenSplit = token.split("\\.");
        tokenSplit[1] = "eyJzdWIiOm51bGwsImF1dGhvcml0aWVzIjpbeyJhdXRob3JpdHkiOiJVU0VSIn1dLCJpYXQiOjE2OTczOTM3MTYsImV4cCI6MTY5NzQyOTcxNn0";
        String nullSubjectToken = String.join(".", tokenSplit);

        // Act
        Optional<String> actual = underTest.extractUsername(nullSubjectToken);

        // Assert
        assertThat(actual).isEmpty();
    }

    @Test
    void extractExpiration_ReturnsExpirationDate_WhenTokenIsValid() {
        // Arrange
        UserRole userRole = new UserRole(UserRole.Role.USER);
        User user = new User(1L, "username", "email", "password", Set.of(userRole));

        String token = underTest.generateToken(user);
        Date expected = new Date(System.currentTimeMillis() + 1000 * 60 * 60 * 10);

        // Act
        Date actual = underTest.extractExpiration(token);

        // Assert
        assertThat(actual).isNotNull();
        assertThat(actual).isCloseTo(expected, 10000L);
    }

    @Test
    void generateToken_ReturnsToken_FromUser() {
        // Arrange
        UserRole userRole = new UserRole(UserRole.Role.USER);
        User user = new User(1L, "username", "email", "password", Set.of(userRole));

        // Act
        String actual = underTest.generateToken(user);

        // Assert
        UserDetailsImpl userDetails = UserDetailsImpl.of(user);
        Boolean tokenValid = underTest.isTokenValid(actual, userDetails);
        assertThat(tokenValid).isTrue();
    }

    @Test
    void generateToken_ReturnsToken_FromUserDetailsImpl() {
        // Arrange
        UserRole userRole = new UserRole(UserRole.Role.USER);
        User user = new User(1L, "username", "email", "password", Set.of(userRole));
        UserDetailsImpl userDetails = UserDetailsImpl.of(user);

        // Act
        String actual = underTest.generateToken(userDetails);

        // Assert
        Boolean tokenValid = underTest.isTokenValid(actual, userDetails);
        assertThat(tokenValid).isTrue();
    }
}