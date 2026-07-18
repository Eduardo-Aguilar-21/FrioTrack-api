package com.mt.friotrackapi.companies.service;

import com.mt.friotrackapi.common.exception.ApiException;
import com.mt.friotrackapi.companies.dto.CompanyResponse;
import com.mt.friotrackapi.companies.entity.CompanyEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class CompanyService {

    @PersistenceContext
    private EntityManager entityManager;

    public List<CompanyResponse> findAll() {
        return entityManager.createQuery("select c from CompanyEntity c order by c.id", CompanyEntity.class)
                .getResultList()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public CompanyResponse findById(Long id) {
        return toResponse(entityById(id));
    }

    public CompanyEntity entityById(Long id) {
        CompanyEntity company = entityManager.find(CompanyEntity.class, id);
        if (company == null) {
            throw new ApiException("Empresa no encontrada");
        }
        return company;
    }

    private CompanyResponse toResponse(CompanyEntity company) {
        return new CompanyResponse(company.getId(), company.getName(), company.getSlug(), company.getStatus());
    }
}
