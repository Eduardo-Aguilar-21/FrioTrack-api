package com.mt.friotrackapi.tracking.dto;

public record TripEventResponse(
        String time,
        String type,
        String title,
        String description,
        String severity
) {}
