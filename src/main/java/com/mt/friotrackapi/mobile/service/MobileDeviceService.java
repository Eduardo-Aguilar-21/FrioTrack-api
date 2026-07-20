package com.mt.friotrackapi.mobile.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.mt.friotrackapi.common.exception.ApiException;
import com.mt.friotrackapi.common.exception.AuthException;
import com.mt.friotrackapi.companies.dto.CompanyResponse;
import com.mt.friotrackapi.companies.service.CompanyService;
import com.mt.friotrackapi.mobile.dto.CreateMobileAccessCodeRequest;
import com.mt.friotrackapi.mobile.dto.LinkMobileDeviceRequest;
import com.mt.friotrackapi.mobile.dto.MobileAccessCodeResponse;
import com.mt.friotrackapi.mobile.dto.MobileSessionResponse;
import com.mt.friotrackapi.persistence.service.JsonStoreService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;

@Service
public class MobileDeviceService {

    private static final String ACCESS_CODES_KEY = "mobile-access-codes";
    private static final String DEVICES_KEY = "mobile-devices";
    private static final int DEFAULT_EXPIRATION_MINUTES = 15;
    private static final int MAX_EXPIRATION_MINUTES = 60;

    private final Path accessCodesPath = Path.of(System.getProperty("user.dir"), "data", "mobile-access-codes.json");
    private final Path devicesPath = Path.of(System.getProperty("user.dir"), "data", "mobile-devices.json");
    private final SecureRandom secureRandom = new SecureRandom();
    private final JsonStoreService jsonStoreService;
    private final CompanyService companyService;

    public MobileDeviceService(JsonStoreService jsonStoreService, CompanyService companyService) {
        this.jsonStoreService = jsonStoreService;
        this.companyService = companyService;
    }

    public synchronized MobileAccessCodeResponse createAccessCode(Long companyId, CreateMobileAccessCodeRequest request) {
        companyService.findById(companyId);
        List<MobileAccessCode> codes = accessCodes();
        Instant now = Instant.now();
        codes.removeIf(code -> Instant.parse(code.expiresAt()).isBefore(now) || code.usedAt() != null);

        String code = uniqueCode(codes, now);
        int minutes = expirationMinutes(request);
        Instant expiresAt = now.plusSeconds(minutes * 60L);
        codes.add(new MobileAccessCode(companyId, code, expiresAt.toString(), null));
        saveAccessCodes(codes);

        return new MobileAccessCodeResponse(companyId, code, expiresAt);
    }

    public synchronized MobileSessionResponse linkDevice(LinkMobileDeviceRequest request) {
        String normalizedCode = normalizeCode(request.code());
        Instant now = Instant.now();
        List<MobileAccessCode> codes = accessCodes();
        MobileAccessCode accessCode = codes.stream()
                .filter(code -> code.usedAt() == null)
                .filter(code -> Instant.parse(code.expiresAt()).isAfter(now))
                .filter(code -> code.code().equals(normalizedCode))
                .findFirst()
                .orElseThrow(() -> new AuthException("Codigo invalido o expirado"));

        String token = createToken();
        CompanyResponse company = companyService.findById(accessCode.companyId());
        String deviceName = cleanDeviceName(request.deviceName());
        List<MobileDevice> devices = devices();
        devices.add(new MobileDevice(token, accessCode.companyId(), deviceName, request.pushToken(), now.toString(), now.toString(), true));

        codes.set(codes.indexOf(accessCode), new MobileAccessCode(accessCode.companyId(), accessCode.code(), accessCode.expiresAt(), now.toString()));
        saveAccessCodes(codes);
        saveDevices(devices);

        return new MobileSessionResponse(token, company.id(), company.name(), deviceName);
    }

    public synchronized MobileDevice authenticate(HttpServletRequest request) {
        String token = tokenFrom(request);
        MobileDevice device = devices().stream()
                .filter(MobileDevice::active)
                .filter(current -> constantTimeEquals(current.token(), token))
                .findFirst()
                .orElseThrow(() -> new AuthException("Token movil invalido"));

        List<MobileDevice> devices = devices();
        devices.set(devices.indexOf(device), new MobileDevice(
                device.token(),
                device.companyId(),
                device.deviceName(),
                device.pushToken(),
                device.createdAt(),
                Instant.now().toString(),
                device.active()
        ));
        saveDevices(devices);

        return device;
    }


    public synchronized List<MobileDevice> activePushDevices(Long companyId) {
        return devices().stream()
                .filter(MobileDevice::active)
                .filter(device -> companyId.equals(device.companyId()))
                .filter(device -> device.pushToken() != null && !device.pushToken().isBlank())
                .toList();
    }

    private String uniqueCode(List<MobileAccessCode> codes, Instant now) {
        for (int attempt = 0; attempt < 100; attempt++) {
            String candidate = String.format(Locale.ROOT, "%04d", secureRandom.nextInt(10_000));
            boolean inUse = codes.stream()
                    .filter(code -> code.usedAt() == null)
                    .filter(code -> Instant.parse(code.expiresAt()).isAfter(now))
                    .anyMatch(code -> code.code().equals(candidate));
            if (!inUse) {
                return candidate;
            }
        }

        throw new ApiException("No se pudo generar un codigo movil");
    }

    private int expirationMinutes(CreateMobileAccessCodeRequest request) {
        if (request == null || request.expirationMinutes() == null) {
            return DEFAULT_EXPIRATION_MINUTES;
        }

        return Math.max(1, Math.min(MAX_EXPIRATION_MINUTES, request.expirationMinutes()));
    }

    private String normalizeCode(String code) {
        return code == null ? "" : code.replaceAll("\\D", "");
    }

    private String cleanDeviceName(String deviceName) {
        if (deviceName == null || deviceName.isBlank()) {
            return "Dispositivo movil";
        }

        return deviceName.trim();
    }

    private String createToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    private String tokenFrom(HttpServletRequest request) {
        String authorization = request.getHeader("Authorization");
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring(7).trim();
        }

        String mobileToken = request.getHeader("X-Mobile-Token");
        if (mobileToken != null && !mobileToken.isBlank()) {
            return mobileToken.trim();
        }

        throw new AuthException("Token movil requerido");
    }

    private List<MobileAccessCode> accessCodes() {
        return new ArrayList<>(jsonStoreService.read(
                ACCESS_CODES_KEY,
                accessCodesPath,
                new TypeReference<List<MobileAccessCode>>() {},
                List::of,
                "No se pudieron cargar los codigos moviles",
                "No se pudieron guardar los codigos moviles"
        ));
    }

    private void saveAccessCodes(List<MobileAccessCode> codes) {
        jsonStoreService.write(ACCESS_CODES_KEY, accessCodesPath, codes, "No se pudieron guardar los codigos moviles");
    }

    private List<MobileDevice> devices() {
        return new ArrayList<>(jsonStoreService.read(
                DEVICES_KEY,
                devicesPath,
                new TypeReference<List<MobileDevice>>() {},
                List::of,
                "No se pudieron cargar los dispositivos moviles",
                "No se pudieron guardar los dispositivos moviles"
        ));
    }

    private void saveDevices(List<MobileDevice> devices) {
        jsonStoreService.write(DEVICES_KEY, devicesPath, devices, "No se pudieron guardar los dispositivos moviles");
    }

    private boolean constantTimeEquals(String left, String right) {
        return java.security.MessageDigest.isEqual(
                left.getBytes(java.nio.charset.StandardCharsets.UTF_8),
                right.getBytes(java.nio.charset.StandardCharsets.UTF_8)
        );
    }

    public record MobileDevice(
            String token,
            Long companyId,
            String deviceName,
            String pushToken,
            String createdAt,
            String lastSeenAt,
            boolean active
    ) {
    }

    private record MobileAccessCode(
            Long companyId,
            String code,
            String expiresAt,
            String usedAt
    ) {
    }
}
