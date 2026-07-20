package com.mt.friotrackapi.tracking.dto;

public record TripReadingResponse(String time, Double temperature, String humidity, String doorState, String coolingUnitState, Double latitude, Double longitude, boolean outOfRange) {}
