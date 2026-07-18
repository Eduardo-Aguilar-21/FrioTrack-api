package com.mt.friotrackapi.alerts.repository;

import com.mt.friotrackapi.alerts.entity.AlertEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AlertRepository extends JpaRepository<AlertEntity, Long> {
    List<AlertEntity> findByCompanyIdOrderByIdDesc(Long companyId);
    List<AlertEntity> findAllByOrderByIdDesc();
    Optional<AlertEntity> findFirstByCompanyIdAndTypeIgnoreCaseAndVehicleCodeIgnoreCaseAndStatusNotIgnoreCase(Long companyId, String type, String vehicleCode, String status);
    List<AlertEntity> findByCompanyIdAndTypeIgnoreCaseAndVehicleCodeIgnoreCaseAndStatusNotIgnoreCase(Long companyId, String type, String vehicleCode, String status);
}
