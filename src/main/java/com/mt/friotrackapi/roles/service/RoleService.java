package com.mt.friotrackapi.roles.service;

import com.mt.friotrackapi.common.exception.ApiException;
import com.mt.friotrackapi.roles.dto.RoleResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RoleService {

    private final List<RoleResponse> roles = List.of(
            new RoleResponse(1L, "ADMIN", "Administrador del sistema"),
            new RoleResponse(2L, "OPERADOR", "Operador de monitoreo"),
            new RoleResponse(3L, "LECTOR", "Usuario de solo lectura")
    );

    public List<RoleResponse> findAll() {
        return roles;
    }

    public RoleResponse findById(Long id) {
        return roles.stream()
                .filter(role -> role.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new ApiException("Rol no encontrado"));
    }
}
