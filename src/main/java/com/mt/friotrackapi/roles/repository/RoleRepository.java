package com.mt.friotrackapi.roles.repository;

import com.mt.friotrackapi.roles.entity.RoleEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<RoleEntity, Long> {
}
