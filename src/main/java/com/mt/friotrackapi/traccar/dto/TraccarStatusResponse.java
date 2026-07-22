package com.mt.friotrackapi.traccar.dto;

import java.util.List;

public record TraccarStatusResponse(
        boolean enabled,
        boolean connected,
        int devices,
        List<String> detectedPaths,
        String lastError
) {
}
