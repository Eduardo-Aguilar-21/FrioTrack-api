package com.mt.friotrackapi.persistence.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.mt.friotrackapi.common.exception.ApiException;
import com.mt.friotrackapi.persistence.entity.AppStoreEntry;
import com.mt.friotrackapi.persistence.repository.AppStoreRepository;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;

@Service
public class JsonStoreService {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final ObjectProvider<AppStoreRepository> repositoryProvider;
    private final String mode;

    public JsonStoreService(ObjectProvider<AppStoreRepository> repositoryProvider, @Value("${friotrack.persistence.mode:db}") String mode) {
        this.repositoryProvider = repositoryProvider;
        this.mode = mode;
    }

    public <T> T read(String key, Path filePath, TypeReference<T> type, Supplier<T> defaults, String loadError, String saveError) {
        if (isDbMode()) {
            AppStoreRepository repository = repository();
            return repository.findById(key)
                    .map(entry -> readPayload(entry.getPayload(), type, loadError))
                    .orElseGet(() -> bootstrap(key, filePath, type, defaults, saveError));
        }

        return readFile(filePath, type, defaults, loadError, saveError);
    }

    public void write(String key, Path filePath, Object value, String errorMessage) {
        if (isDbMode()) {
            writeDb(key, value, errorMessage);
            return;
        }

        writeFile(filePath, value, errorMessage);
    }

    private <T> T bootstrap(String key, Path filePath, TypeReference<T> type, Supplier<T> defaults, String saveError) {
        T value = null;
        if (Files.exists(filePath)) {
            try {
                value = objectMapper.readValue(filePath.toFile(), type);
            } catch (IOException ignored) {
                value = null;
            }
        }

        if (value == null) {
            value = defaults.get();
        }

        writeDb(key, value, saveError);
        return value;
    }

    private <T> T readFile(Path filePath, TypeReference<T> type, Supplier<T> defaults, String loadError, String saveError) {
        try {
            if (Files.exists(filePath)) {
                return objectMapper.readValue(filePath.toFile(), type);
            }

            T value = defaults.get();
            writeFile(filePath, value, saveError);
            return value;
        } catch (IOException ex) {
            throw new ApiException(loadError);
        }
    }

    private <T> T readPayload(String payload, TypeReference<T> type, String errorMessage) {
        try {
            return objectMapper.readValue(payload, type);
        } catch (IOException ex) {
            throw new ApiException(errorMessage);
        }
    }

    private void writeDb(String key, Object value, String errorMessage) {
        try {
            AppStoreRepository repository = repository();
            String payload = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(value);
            AppStoreEntry entry = repository.findById(key).orElseGet(() -> new AppStoreEntry(key, payload));
            entry.updatePayload(payload);
            repository.save(entry);
        } catch (Exception ex) {
            String detail = ex.getMessage() == null ? ex.getClass().getSimpleName() : ex.getMessage();
            throw new ApiException(errorMessage + ": " + detail);
        }
    }

    private void writeFile(Path path, Object value, String errorMessage) {
        try {
            Files.createDirectories(path.getParent());
            objectMapper.writerWithDefaultPrettyPrinter().writeValue(path.toFile(), value);
        } catch (IOException ex) {
            throw new ApiException(errorMessage);
        }
    }

    private boolean isDbMode() {
        return "db".equalsIgnoreCase(mode);
    }

    private AppStoreRepository repository() {
        AppStoreRepository repository = repositoryProvider.getIfAvailable();
        if (repository == null) {
            throw new ApiException("Persistencia PostgreSQL no disponible");
        }
        return repository;
    }
}
