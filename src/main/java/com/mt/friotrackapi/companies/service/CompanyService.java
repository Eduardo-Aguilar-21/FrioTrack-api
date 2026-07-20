package com.mt.friotrackapi.companies.service;

import com.mt.friotrackapi.common.exception.ApiException;
import com.mt.friotrackapi.companies.dto.CompanyResponse;
import com.mt.friotrackapi.companies.dto.CreateCompanyRequest;
import com.mt.friotrackapi.companies.entity.CompanyEntity;
import com.mt.friotrackapi.companies.repository.CompanyRepository;
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

    private final CompanyRepository companyRepository;

    public CompanyService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

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


    @Transactional
    public CompanyResponse create(CreateCompanyRequest request) {
        String slug = normalizeSlug(request.taxId());
        if (companyRepository.existsBySlug(slug)) {
            throw new ApiException("Ya existe una empresa con ese identificador");
        }

        CompanyEntity company = new CompanyEntity(
                clean(request.name()),
                slug,
                normalizeStatus(request.status()),
                request.warningOfflineMinutes(),
                request.criticalOfflineMinutes()
        );
        return toResponse(companyRepository.save(company));
    }

    @Transactional
    public CompanyResponse update(Long id, CreateCompanyRequest request) {
        CompanyEntity company = entityById(id);
        String slug = normalizeSlug(request.taxId());
        if (companyRepository.existsBySlugAndIdNot(slug, id)) {
            throw new ApiException("Ya existe una empresa con ese identificador");
        }

        company.update(
                clean(request.name()),
                slug,
                normalizeStatus(request.status()),
                request.warningOfflineMinutes(),
                request.criticalOfflineMinutes()
        );
        return toResponse(company);
    }

    @Transactional
    public CompanyResponse setStatus(Long id, String status) {
        CompanyEntity company = entityById(id);
        company.setStatus(normalizeStatus(status));
        return toResponse(company);
    }

    @Transactional
    public void delete(Long id) {
        CompanyEntity company = entityById(id);
        Long users = entityManager.createQuery("select count(u) from UserEntity u where u.company.id = :companyId", Long.class)
                .setParameter("companyId", id)
                .getSingleResult();
        Long vehicles = entityManager.createQuery("select count(v) from VehicleEntity v where v.company.id = :companyId", Long.class)
                .setParameter("companyId", id)
                .getSingleResult();
        if (users > 0 || vehicles > 0) {
            throw new ApiException("No se puede eliminar una empresa con usuarios o vehiculos asociados");
        }
        companyRepository.delete(company);
    }

    public CompanyEntity entityById(Long id) {
        CompanyEntity company = entityManager.find(CompanyEntity.class, id);
        if (company == null) {
            throw new ApiException("Empresa no encontrada");
        }
        return company;
    }

    private CompanyResponse toResponse(CompanyEntity company) {
        return new CompanyResponse(company.getId(), company.getName(), company.getSlug(), company.getStatus(), company.getWarningOfflineMinutes(), company.getCriticalOfflineMinutes());
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeSlug(String value) {
        return clean(value).toLowerCase();
    }

    private String normalizeStatus(String value) {
        String normalized = clean(value).toUpperCase();
        if (!normalized.equals("ACTIVE") && !normalized.equals("INACTIVE")) {
            throw new ApiException("Estado de empresa invalido");
        }
        return normalized;
    }
}
