package com.mt.friotrackapi.vehicles.repository;

import com.mt.friotrackapi.vehicles.entity.VehicleEntity;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VehicleRepository extends JpaRepository<VehicleEntity, Long> {
    List<VehicleEntity> findByCompanyIdOrderByIdAsc(Long companyId);
    Optional<VehicleEntity> findFirstByDeviceIdIgnoreCase(String deviceId);
    boolean existsByCompanyIdAndCodeIgnoreCaseOrCompanyIdAndPlateIgnoreCase(Long companyId1, String code, Long companyId2, String plate);
    boolean existsByCompanyIdAndCodeIgnoreCaseAndIdNot(Long companyId, String code, Long id);
    boolean existsByCompanyIdAndPlateIgnoreCaseAndIdNot(Long companyId, String plate, Long id);
}
