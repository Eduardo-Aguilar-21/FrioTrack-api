package com.mt.friotrackapi.companies.repository;

import com.mt.friotrackapi.companies.entity.CompanyEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CompanyRepository extends JpaRepository<CompanyEntity, Long> {
}
