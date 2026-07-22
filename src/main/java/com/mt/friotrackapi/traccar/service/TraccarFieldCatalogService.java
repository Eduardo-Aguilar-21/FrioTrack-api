package com.mt.friotrackapi.traccar.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.mt.friotrackapi.persistence.service.JsonStoreService;
import com.mt.friotrackapi.vehicles.entity.VehicleEntity;
import org.springframework.stereotype.Service;

import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TraccarFieldCatalogService {
    private static final Duration REFRESH_INTERVAL = Duration.ofMinutes(5);
    private final Path storePath = Path.of(System.getProperty("user.dir"), "data", "traccar-field-catalog.json");
    private final JsonStoreService jsonStoreService;
    private final Map<String, Map<String, CatalogField>> catalog;

    public TraccarFieldCatalogService(JsonStoreService jsonStoreService) {
        this.jsonStoreService = jsonStoreService;
        this.catalog = new LinkedHashMap<>(jsonStoreService.read(
                "traccar-field-catalog",
                storePath,
                new TypeReference<Map<String, Map<String, CatalogField>>>() {},
                LinkedHashMap::new,
                "No se pudo cargar el catalogo Traccar",
                "No se pudo persistir el catalogo Traccar"
        ));
    }

    public synchronized void record(VehicleEntity vehicle, JsonNode position) {
        String key = vehicle.getCompany().getId() + ":" + vehicle.getId() + ":" + clean(vehicle.getModel(), "sin-modelo");
        Map<String, CatalogField> fields = catalog.computeIfAbsent(key, ignored -> new LinkedHashMap<>());
        String now = Instant.now().toString();
        boolean changed = false;

        var names = position.fieldNames();
        while (names.hasNext()) {
            String field = names.next();
            if (!"attributes".equals(field)) changed |= recordField(fields, field, position.get(field), now);
        }

        JsonNode attributes = position.path("attributes");
        if (attributes.isObject()) {
            var attributeNames = attributes.fieldNames();
            while (attributeNames.hasNext()) {
                String field = attributeNames.next();
                changed |= recordField(fields, "attributes." + field, attributes.get(field), now);
            }
        }

        if (changed) {
            jsonStoreService.write("traccar-field-catalog", storePath, catalog, "No se pudo persistir el catalogo Traccar");
        }
    }

    public synchronized List<String> paths(Long companyId) {
        String prefix = companyId == null ? "" : companyId + ":";
        return catalog.entrySet().stream()
                .filter(entry -> companyId == null || entry.getKey().startsWith(prefix))
                .flatMap(entry -> entry.getValue().keySet().stream())
                .distinct()
                .sorted()
                .toList();
    }

    private boolean recordField(Map<String, CatalogField> fields, String path, JsonNode value, String now) {
        CatalogField current = fields.get(path);
        String type = dataType(value);
        String sample = sample(value);
        if (current != null
                && current.dataType().equals(type)
                && current.sampleValue().equals(sample)
                && Duration.between(Instant.parse(current.lastSeenAt()), Instant.parse(now)).compareTo(REFRESH_INTERVAL) < 0) {
            return false;
        }
        fields.put(path, new CatalogField(path, type, sample, now));
        return true;
    }

    private String dataType(JsonNode value) {
        if (value != null && value.isBoolean()) return "BOOLEAN";
        if (value != null && value.isNumber()) return "NUMBER";
        return "TEXT";
    }

    private String sample(JsonNode value) {
        if (value == null || value.isNull()) return "";
        String text = value.isValueNode() ? value.asText() : value.toString();
        return text.length() > 160 ? text.substring(0, 160) : text;
    }

    private String clean(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value.trim().toLowerCase(java.util.Locale.ROOT);
    }

    public record CatalogField(String path, String dataType, String sampleValue, String lastSeenAt) {}
}
