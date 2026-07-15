package com.mt.friotrackapi.common.util;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;

public final class DateTimeUtils {
    public static final ZoneId LIMA_ZONE = ZoneId.of("America/Lima");

    private DateTimeUtils() {
    }

    public static ZonedDateTime nowInLima() {
        return Instant.now().atZone(LIMA_ZONE);
    }
}
