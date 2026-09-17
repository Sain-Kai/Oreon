package com.ragplatform.security;

import io.jsonwebtoken.Claims;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceTest {

    private final JwtService jwtService = new JwtService(
            "test-secret-key-that-is-long-enough-for-hs256-signing-1234567890",
            86_400_000L);

    @Test
    void generatesTokenThatRoundTripsTenantAndUserClaims() {
        UUID tenantId = UUID.randomUUID();
        AppUser user = new AppUser(tenantId, "alice", "hashed-password", Role.ADMIN);
        // id is normally assigned by JPA on save - simulate it here via reflection-free test setup
        // by generating the token from a UserPrincipal built directly around this in-memory user.
        UserPrincipal principal = new UserPrincipal(user) {
            @Override
            public UUID getId() {
                return UUID.fromString("11111111-1111-1111-1111-111111111111");
            }
        };

        String token = jwtService.generateToken(principal);
        Claims claims = jwtService.parseClaims(token);

        assertThat(claims.getSubject()).isEqualTo("alice");
        assertThat(jwtService.extractTenantId(claims)).isEqualTo(tenantId);
        assertThat(claims.get("role", String.class)).isEqualTo("ROLE_ADMIN");
    }
}
