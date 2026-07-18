package com.mt.friotrackapi.sensors.repository;

import com.mt.friotrackapi.sensors.entity.SensorParameterEntity;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SensorParameterRepository extends JpaRepository<SensorParameterEntity, Long> {
    List<SensorParameterEntity> findByCompanyIdOrderByIdAsc(Long companyId);
    boolean existsByCompanyIdAndCodeIgnoreCase(Long companyId, String code);
    boolean existsByCompanyIdAndCodeIgnoreCaseAndIdNot(Long companyId, String code, Long id);
}
