package com.mt.friotrackapi.tracking.dto;

import java.util.List;

public record TripResponse(
        String id, String name, String dateLabel, String windowLabel,
        String startedAt, String endedAt, String status,
        Double startLatitude, Double startLongitude, Double endLatitude, Double endLongitude,
        List<TripReadingResponse> readings, String distanceKm, String averageTemperature,
        String minTemperature, String maxTemperature, int inRangePercent, int outOfRangePercent,
        int doorOpenings, String duration, List<TripEventResponse> events
) {}
