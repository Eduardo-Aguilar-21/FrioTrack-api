package com.mt.friotrackapi.roles.controller;

import com.mt.friotrackapi.common.response.ApiResponse;
import com.mt.friotrackapi.roles.dto.RoleResponse;
import com.mt.friotrackapi.roles.service.RoleService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/roles")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping
    public ApiResponse<List<RoleResponse>> findAll() {
        return ApiResponse.ok(roleService.findAll());
    }

    @GetMapping("/{id}")
    public ApiResponse<RoleResponse> findById(@PathVariable Long id) {
        return ApiResponse.ok(roleService.findById(id));
    }
}
