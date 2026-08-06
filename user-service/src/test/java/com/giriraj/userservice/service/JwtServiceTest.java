package com.giriraj.userservice.service;

import com.giriraj.userservice.entity.User;
import com.giriraj.userservice.enums.UserRole;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.crypto.SecretKey;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private static final String SECRET =
            "c3VwZXItc2VjcmV0LWtleS1mb3Itand0LXNpZ25pbmctMjAyNg==";

    private static final long EXPIRATION_MS =
            3_600_000L;

    private JwtService jwtService;
    private SecretKey secretKey;
    private User user;

    @BeforeEach
    void setUp() {

        jwtService = new JwtService(
                SECRET,
                EXPIRATION_MS
        );

        secretKey = Keys.hmacShaKeyFor(
                Decoders.BASE64.decode(SECRET)
        );

        user = new User();
        user.setId(1L);
        user.setEmail("giriraj@example.com");
        user.setRole(UserRole.CUSTOMER);
    }

    @Test
    void generateToken_shouldContainExpectedClaims() {

        String token =
                jwtService.generateToken(user);

        assertNotNull(token);
        assertFalse(token.isBlank());

        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        assertEquals(
                "1",
                claims.getSubject()
        );

        Object userIdClaim =
                claims.get("userId");

        assertInstanceOf(
                Number.class,
                userIdClaim
        );

        assertEquals(
                1L,
                ((Number) userIdClaim).longValue()
        );

        assertEquals(
                "giriraj@example.com",
                claims.get("email", String.class)
        );

        assertEquals(
                "CUSTOMER",
                claims.get("role", String.class)
        );

        assertNotNull(claims.getIssuedAt());
        assertNotNull(claims.getExpiration());

        assertTrue(
                claims.getExpiration()
                        .after(new Date())
        );
    }

    @Test
    void generateToken_shouldUseConfiguredExpiration() {

        String token =
                jwtService.generateToken(user);

        Claims claims = Jwts.parser()
                .verifyWith(secretKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();

        long actualLifetime =
                claims.getExpiration().getTime()
                        - claims.getIssuedAt().getTime();

        /*
         * JWT date precision can remove a few
         * milliseconds during serialization.
         */
        assertTrue(
                actualLifetime
                        >= EXPIRATION_MS - 1_000
        );

        assertTrue(
                actualLifetime
                        <= EXPIRATION_MS
        );
    }
}