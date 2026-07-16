package com.mt.friotrackapi.protocol.controller;

import com.mt.friotrackapi.common.response.ApiResponse;
import com.mt.friotrackapi.protocol.dto.ProtocolConfigResponse;
import com.mt.friotrackapi.protocol.dto.SaveProtocolConfigRequest;
import com.mt.friotrackapi.protocol.service.ProtocolConfigService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/protocol-config")
public class ProtocolConfigController {

    private final ProtocolConfigService protocolConfigService;

    public ProtocolConfigController(ProtocolConfigService protocolConfigService) {
        this.protocolConfigService = protocolConfigService;
    }

    @GetMapping
    public ApiResponse<ProtocolConfigResponse> findByCompany(@RequestParam Long companyId) {
        return ApiResponse.ok(protocolConfigService.findByCompany(companyId));
    }

    @PutMapping
    public ApiResponse<ProtocolConfigResponse> save(@Valid @RequestBody SaveProtocolConfigRequest request) {
        return ApiResponse.ok("Configuracion de protocolo actualizada", protocolConfigService.save(request));
    }
}
