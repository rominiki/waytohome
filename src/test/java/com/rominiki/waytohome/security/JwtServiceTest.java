package com.rominiki.waytohome.security;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.List;
import static org.assertj.core.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        jwtService.secretKey = "mySecretKey1234567890mySecretKey1234567890";
        jwtService.expiration = 86400000L;
        userDetails = new User("test@example.com", "hashedpassword", List.of());
    }
    @Test
    void generateToken_thenExtractEmail_matches() {
        String token = jwtService.generateToken(userDetails);
        String email = jwtService.extractEmail(token);
        assertThat(email).isEqualTo("test@example.com");
    }
    @Test
    void isTokenValid_withCorrectUser_returnsTrue() {
        String token = jwtService.generateToken(userDetails);
        assertThat(jwtService.isTokenValid(token, userDetails)).isTrue();
    }
    @Test
    void isTokenValid_withWrongUser_returnsFalse() {
        String token = jwtService.generateToken(userDetails);
        UserDetails otherUser = new User("other@example.com", "hash", List.of());
        assertThat(jwtService.isTokenValid(token, otherUser)).isFalse();
    }
    @Test
    void expiredToken_isNotValid() {
        jwtService.expiration = -1000L;
        String token = jwtService.generateToken(userDetails);
        assertThat(jwtService.isTokenValid(token, userDetails)).isFalse();
    }
}