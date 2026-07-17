package com.mt.friotrackapi.protocol.controller;

import com.mt.friotrackapi.auth.service.TenantAccessService;
import com.mt.friotrackapi.common.response.ApiResponse;
import com.mt.friotrackapi.protocol.dto.ProtocolConfigResponse;
import com.mt.friotrackapi.protocol.dto.SaveProtocolConfigRequest;
import com.mt.friotrackapi.protocol.service.ProtocolConfigService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/protocol-config")
public class ProtocolConfigController {

    private final ProtocolConfigService protocolConfigService;
    private final TenantAccessService tenantAccessService;

    public ProtocolConfigController(ProtocolConfigService protocolConfigService, TenantAccessService tenantAccessService) {
        this.protocolConfigService = protocolConfigService;
        this.tenantAccessService = tenantAccessService;
    }

    @GetMapping
    public ApiResponse<ProtocolConfigResponse> findByCompany() {
        return ApiResponse.ok(protocolConfigService.findByCompany(tenantAccessService.companyId()));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping
    public ApiResponse<ProtocolConfigResponse> save(@Valid @RequestBody SaveProtocolConfigRequest request) {
        SaveProtocolConfigRequest scoped = new SaveProtocolConfigRequest(
                tenantAccessService.companyId(),
                request.brokerName(),
                request.topicPattern(),
                request.payloadRoot(),
                request.fields(),
                request.temperatureRules()
        );
        return ApiResponse.ok("Configuracion de protocolo actualizada", protocolConfigService.save(scoped));
    }
}
