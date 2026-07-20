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

    public boolean isServiceAdmin() {
        return "SA".equalsIgnoreCase(currentUserService.currentUser().role());
    }

    public Long resolveCompanyId(Long requestedCompanyId) {
        if (isServiceAdmin()) {
            return requestedCompanyId != null ? requestedCompanyId : companyId();
        }
        Long currentCompanyId = companyId();
        if (requestedCompanyId != null && !currentCompanyId.equals(requestedCompanyId)) {
            throw new ForbiddenException("No tienes acceso a esta empresa");
        }
        return currentCompanyId;
    }

    public void requireCompany(Long companyId) {
        if (isServiceAdmin()) {
            return;
        }
        Long currentCompanyId = currentUserService.companyId();
        if (companyId == null || !currentCompanyId.equals(companyId)) {
            throw new ForbiddenException("No tienes acceso a esta empresa");
        }
    }
}
