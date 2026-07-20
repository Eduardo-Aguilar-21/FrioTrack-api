package com.mt.friotrackapi.users.controller;

import com.mt.friotrackapi.auth.service.CurrentUserService;
import com.mt.friotrackapi.auth.service.TenantAccessService;
import com.mt.friotrackapi.common.response.ApiResponse;
import com.mt.friotrackapi.users.dto.ChangePasswordRequest;
import com.mt.friotrackapi.users.dto.CreateUserRequest;
import com.mt.friotrackapi.users.dto.UserResponse;
import com.mt.friotrackapi.users.service.UserService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final TenantAccessService tenantAccessService;
    private final CurrentUserService currentUserService;

    public UserController(UserService userService, TenantAccessService tenantAccessService, CurrentUserService currentUserService) {
        this.userService = userService;
        this.tenantAccessService = tenantAccessService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ApiResponse<List<UserResponse>> findAll(@RequestParam(required = false) Long companyId) {
        if (tenantAccessService.isServiceAdmin() && companyId == null) {
            return ApiResponse.ok(userService.findAll(null));
        }
        return ApiResponse.ok(userService.findAll(tenantAccessService.resolveCompanyId(companyId)));
    }

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> findById(@PathVariable Long id) {
        UserResponse user = userService.findById(id);
        tenantAccessService.requireCompany(user.companyId());
        return ApiResponse.ok(user);
    }


    @PatchMapping("/me/password")
    public ApiResponse<Void> changeOwnPassword(@Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(currentUserService.currentUser().id(), request.currentPassword(), request.newPassword());
        return ApiResponse.ok("Contraseña actualizada", null);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SA')")
    @PostMapping
    public ApiResponse<UserResponse> create(@Valid @RequestBody CreateUserRequest request) {
        Long companyId = tenantAccessService.resolveCompanyId(request.companyId());
        tenantAccessService.requireCompany(companyId);
        CreateUserRequest scoped = new CreateUserRequest(companyId, request.username(), request.name(), request.email(), request.password(), request.roleId());
        return ApiResponse.ok("Usuario creado", userService.create(scoped));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SA')")
    @PutMapping("/{id}")
    public ApiResponse<UserResponse> update(@PathVariable Long id, @Valid @RequestBody CreateUserRequest request) {
        tenantAccessService.requireCompany(userService.findById(id).companyId());
        Long companyId = tenantAccessService.resolveCompanyId(request.companyId());
        tenantAccessService.requireCompany(companyId);
        CreateUserRequest scoped = new CreateUserRequest(companyId, request.username(), request.name(), request.email(), request.password(), request.roleId());
        return ApiResponse.ok("Usuario actualizado", userService.update(id, scoped));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SA')")
    @PatchMapping("/{id}/status/{status}")
    public ApiResponse<UserResponse> setStatus(@PathVariable Long id, @PathVariable String status) {
        tenantAccessService.requireCompany(userService.findById(id).companyId());
        return ApiResponse.ok("Estado de usuario actualizado", userService.setStatus(id, status));
    }
}
