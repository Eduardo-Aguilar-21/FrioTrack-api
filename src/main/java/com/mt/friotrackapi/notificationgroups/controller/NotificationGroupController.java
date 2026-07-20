package com.mt.friotrackapi.notificationgroups.controller;

import com.mt.friotrackapi.auth.service.TenantAccessService;
import com.mt.friotrackapi.common.response.ApiResponse;
import com.mt.friotrackapi.notificationgroups.dto.CreateNotificationGroupRequest;
import com.mt.friotrackapi.notificationgroups.dto.NotificationGroupResponse;
import com.mt.friotrackapi.notificationgroups.service.NotificationGroupService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notification-groups")
public class NotificationGroupController {

    private final NotificationGroupService notificationGroupService;
    private final TenantAccessService tenantAccessService;

    public NotificationGroupController(NotificationGroupService notificationGroupService, TenantAccessService tenantAccessService) {
        this.notificationGroupService = notificationGroupService;
        this.tenantAccessService = tenantAccessService;
    }

    @GetMapping
    public ApiResponse<List<NotificationGroupResponse>> findAll(@RequestParam(required = false) Long companyId) {
        if (tenantAccessService.isServiceAdmin() && companyId == null) {
            return ApiResponse.ok(notificationGroupService.findAll(null));
        }
        return ApiResponse.ok(notificationGroupService.findAll(tenantAccessService.resolveCompanyId(companyId)));
    }

    @GetMapping("/{id}")
    public ApiResponse<NotificationGroupResponse> findById(@PathVariable Long id) {
        NotificationGroupResponse group = notificationGroupService.findById(id);
        tenantAccessService.requireCompany(group.companyId());
        return ApiResponse.ok(group);
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SA')")
    @PostMapping
    public ApiResponse<NotificationGroupResponse> create(@Valid @RequestBody CreateNotificationGroupRequest request) {
        CreateNotificationGroupRequest scoped = scopedRequest(request);
        return ApiResponse.ok("Grupo de notificaciones creado", notificationGroupService.create(scoped));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SA')")
    @PutMapping("/{id}")
    public ApiResponse<NotificationGroupResponse> update(@PathVariable Long id, @Valid @RequestBody CreateNotificationGroupRequest request) {
        NotificationGroupResponse current = notificationGroupService.findById(id);
        tenantAccessService.requireCompany(current.companyId());
        CreateNotificationGroupRequest scoped = scopedRequest(request);
        return ApiResponse.ok("Grupo de notificaciones actualizado", notificationGroupService.update(id, scoped));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SA')")
    @PatchMapping("/{id}/status/{status}")
    public ApiResponse<NotificationGroupResponse> setStatus(@PathVariable Long id, @PathVariable String status) {
        NotificationGroupResponse current = notificationGroupService.findById(id);
        tenantAccessService.requireCompany(current.companyId());
        return ApiResponse.ok("Estado de grupo actualizado", notificationGroupService.setStatus(id, status));
    }

    @PreAuthorize("hasAnyRole('ADMIN', 'SA')")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        NotificationGroupResponse current = notificationGroupService.findById(id);
        tenantAccessService.requireCompany(current.companyId());
        notificationGroupService.delete(id);
        return ApiResponse.ok("Grupo de notificaciones eliminado", null);
    }

    private CreateNotificationGroupRequest scopedRequest(CreateNotificationGroupRequest request) {
        Long companyId = tenantAccessService.resolveCompanyId(request.companyId());
        tenantAccessService.requireCompany(companyId);
        return new CreateNotificationGroupRequest(companyId, request.name(), request.description(), request.alertTypes(), request.severities(), request.channels(), request.userIds(), request.vehicleIds(), request.status());
    }
}
