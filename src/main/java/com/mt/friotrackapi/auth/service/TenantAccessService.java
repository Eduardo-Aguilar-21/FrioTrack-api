package com.mt.friotrackapi.auth.service;

import com.mt.friotrackapi.common.exception.ForbiddenException;
import org.springframework.stereotype.Service;

@Service
public class TenantAccessService {

    private final CurrentUserService currentUserService;

    public TenantAccessService(CurrentUserService currentUserService) {
        this.currentUserService = currentUserService;
    }

    public Long companyId() {
        return currentUserService.companyId();
    }

    public void requireCompany(Long companyId) {
        Long currentCompanyId = currentUserService.companyId();
        if (companyId == null || !currentCompanyId.equals(companyId)) {
            throw new ForbiddenException("No tienes acceso a esta empresa");
        }
    }
}
