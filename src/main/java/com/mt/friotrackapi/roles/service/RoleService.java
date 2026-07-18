package com.mt.friotrackapi.roles.service;

import com.mt.friotrackapi.common.exception.ApiException;
import com.mt.friotrackapi.roles.dto.RoleResponse;
import com.mt.friotrackapi.roles.entity.RoleEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional(readOnly = true)
public class RoleService {

    @PersistenceContext
    private EntityManager entityManager;

    public List<RoleResponse> findAll() {
        return entityManager.createQuery("select r from RoleEntity r order by r.id", RoleEntity.class)
                .getResultList()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    public RoleResponse findById(Long id) {
        return toResponse(entityById(id));
    }

    public RoleEntity entityById(Long id) {
        RoleEntity role = entityManager.find(RoleEntity.class, id);
        if (role == null) {
            throw new ApiException("Rol no encontrado");
        }
        return role;
    }

    private RoleResponse toResponse(RoleEntity role) {
        return new RoleResponse(role.getId(), role.getName(), role.getDescription());
    }
}
