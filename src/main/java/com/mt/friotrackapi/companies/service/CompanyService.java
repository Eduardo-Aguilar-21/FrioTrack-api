package com.mt.friotrackapi.companies.service;

import com.mt.friotrackapi.common.exception.ApiException;
import com.mt.friotrackapi.companies.dto.CompanyResponse;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CompanyService {

    private final List<CompanyResponse> companies = List.of(
            new CompanyResponse(1L, "FrioTrack Demo", "20123456789", "ACTIVE"),
            new CompanyResponse(2L, "Cadena Fria Norte", "20987654321", "ACTIVE")
    );

    public List<CompanyResponse> findAll() {
        return companies;
    }

    public CompanyResponse findById(Long id) {
        return companies.stream()
                .filter(company -> company.id().equals(id))
                .findFirst()
                .orElseThrow(() -> new ApiException("Empresa no encontrada"));
    }
}
