package com.mt.friotrackapi.auth.service;

import com.mt.friotrackapi.common.exception.AuthException;
import com.mt.friotrackapi.users.dto.UserResponse;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class AuthTokenServiceTest {

    @Test
    void createsAndParsesSignedToken() {
        AuthTokenService service = new AuthTokenService("test-secret", 60);
        UserResponse user = new UserResponse(7L, 3L, "Empresa", "admin", "Admin", "admin@test.pe", "ADMIN", "ACTIVE");

        var parsed = service.parseToken(service.createToken(user));

        assertEquals(7L, parsed.id());
        assertEquals(3L, parsed.companyId());
        assertEquals("admin", parsed.username());
        assertEquals("ADMIN", parsed.role());
    }

    @Test
    void rejectsExpiredToken() {
        AuthTokenService service = new AuthTokenService("test-secret", -1);
        UserResponse user = new UserResponse(7L, 3L, "Empresa", "admin", "Admin", "admin@test.pe", "ADMIN", "ACTIVE");

        String token = service.createToken(user);

        assertThrows(AuthException.class, () -> service.parseToken(token));
    }
}
