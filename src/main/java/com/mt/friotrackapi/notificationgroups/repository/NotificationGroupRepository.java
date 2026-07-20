package com.mt.friotrackapi.notificationgroups.repository;

import com.mt.friotrackapi.notificationgroups.entity.NotificationGroupEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationGroupRepository extends JpaRepository<NotificationGroupEntity, Long> {
    List<NotificationGroupEntity> findByCompanyIdOrderByIdAsc(Long companyId);
    boolean existsByCompanyIdAndNameIgnoreCase(Long companyId, String name);
    boolean existsByCompanyIdAndNameIgnoreCaseAndIdNot(Long companyId, String name, Long id);
}
