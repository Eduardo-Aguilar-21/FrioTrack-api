package com.mt.friotrackapi.auth.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mt.friotrackapi.auth.dto.AuthenticatedUser;
import com.mt.friotrackapi.common.exception.AuthException;
import com.mt.friotrackapi.users.dto.UserResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
public class AuthTokenService {

    private static final String HMAC_ALGORITHM = "HmacSHA256";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final String secret;
    private final long expirationSeconds;

    public AuthTokenService(
            @Value("${friotrack.auth.jwt-secret:friotrack-dev-secret-change-me}") String secret,
            @Value("${friotrack.auth.jwt-expiration-seconds:28800}") long expirationSeconds
    ) {
        this.secret = secret;
        this.expirationSeconds = expirationSeconds;
    }

    public String createToken(UserResponse user) {
        try {
            Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("sub", user.id());
            payload.put("companyId", user.companyId());
            payload.put("username", user.username());
            payload.put("role", user.role());
            payload.put("exp", Instant.now().getEpochSecond() + expirationSeconds);

            String headerPart = encode(objectMapper.writeValueAsBytes(header));
            String payloadPart = encode(objectMapper.writeValueAsBytes(payload));
            String signature = sign(headerPart + "." + payloadPart);
            return headerPart + "." + payloadPart + "." + signature;
        } catch (Exception ex) {
            throw new AuthException("No se pudo crear el token");
        }
    }

    public AuthenticatedUser parseToken(String token) {
        try {
            String[] parts = token == null ? new String[0] : token.split("\\.");
            if (parts.length != 3) {
                throw new AuthException("Token invalido");
            }

            String expectedSignature = sign(parts[0] + "." + parts[1]);
            if (!constantTimeEquals(expectedSignature, parts[2])) {
                throw new AuthException("Token invalido");
            }

            Map<String, Object> payload = objectMapper.readValue(decode(parts[1]), new TypeReference<>() {});
            long exp = numberValue(payload.get("exp"));
            if (Instant.now().getEpochSecond() >= exp) {
                throw new AuthException("Sesion expirada");
            }

            return new AuthenticatedUser(
                    numberValue(payload.get("sub")),
                    numberValue(payload.get("companyId")),
                    String.valueOf(payload.get("username")),
                    String.valueOf(payload.get("role"))
            );
        } catch (AuthException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new AuthException("Token invalido");
        }
    }

    private String sign(String value) throws Exception {
        Mac mac = Mac.getInstance(HMAC_ALGORITHM);
        mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
        return encode(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }

    private String encode(byte[] value) {
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private byte[] decode(String value) {
        return Base64.getUrlDecoder().decode(value);
    }

    private long numberValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }

    private boolean constantTimeEquals(String left, String right) {
        return java.security.MessageDigest.isEqual(
                left.getBytes(StandardCharsets.UTF_8),
                right.getBytes(StandardCharsets.UTF_8)
        );
    }
}
