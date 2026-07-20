package com.mt.friotrackapi.realtime.controller;

import com.mt.friotrackapi.auth.dto.AuthenticatedUser;
import com.mt.friotrackapi.auth.service.AuthTokenService;
import com.mt.friotrackapi.common.exception.ForbiddenException;
import com.mt.friotrackapi.realtime.service.RealtimeEventService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequestMapping("/api/realtime")
public class RealtimeController {
    private final AuthTokenService authTokenService;
    private final RealtimeEventService realtimeEventService;

    public RealtimeController(AuthTokenService authTokenService, RealtimeEventService realtimeEventService) {
        this.authTokenService = authTokenService;
        this.realtimeEventService = realtimeEventService;
    }

    @GetMapping("/stream")
    public SseEmitter stream(@RequestParam String token, @RequestParam(required = false) Long companyId) {
        AuthenticatedUser user = authTokenService.parseToken(token);
        Long targetCompanyId = companyId == null ? user.companyId() : companyId;
        if (!"SA".equalsIgnoreCase(user.role()) && !user.companyId().equals(targetCompanyId)) {
            throw new ForbiddenException("No tienes acceso a esta empresa");
        }
        return realtimeEventService.subscribe(targetCompanyId);
    }
}
