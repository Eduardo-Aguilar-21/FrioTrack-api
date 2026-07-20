package com.mt.friotrackapi.notifications.controller;

import com.mt.friotrackapi.auth.dto.AuthenticatedUser;
import com.mt.friotrackapi.auth.service.CurrentUserService;
import com.mt.friotrackapi.auth.service.TenantAccessService;
import com.mt.friotrackapi.common.response.ApiResponse;
import com.mt.friotrackapi.notifications.dto.NotificationDeliveryResponse;
import com.mt.friotrackapi.notifications.service.NotificationDeliveryService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/notification-deliveries")
public class NotificationDeliveryController {
    private final NotificationDeliveryService notificationDeliveryService;
    private final TenantAccessService tenantAccessService;
    private final CurrentUserService currentUserService;

    public NotificationDeliveryController(NotificationDeliveryService notificationDeliveryService, TenantAccessService tenantAccessService, CurrentUserService currentUserService) {
        this.notificationDeliveryService = notificationDeliveryService;
        this.tenantAccessService = tenantAccessService;
        this.currentUserService = currentUserService;
    }

    @GetMapping
    public ApiResponse<List<NotificationDeliveryResponse>> findAll(@RequestParam(required = false) Long companyId, @RequestParam(required = false) Long userId, @RequestParam(required = false) Long alertId) {
        AuthenticatedUser user = currentUserService.currentUser();
        Long scopedCompanyId = tenantAccessService.resolveCompanyId(companyId);
        tenantAccessService.requireCompany(scopedCompanyId);
        Long scopedUserId = canAuditAll(user.role()) ? userId : user.id();
        return ApiResponse.ok(notificationDeliveryService.findAll(scopedCompanyId, scopedUserId, alertId));
    }

    private boolean canAuditAll(String role) {
        return "ADMIN".equalsIgnoreCase(role) || "SA".equalsIgnoreCase(role);
    }
}
