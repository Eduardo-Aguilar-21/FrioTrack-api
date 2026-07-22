package com.mt.friotrackapi.common.dto;
import java.util.List;
public record PageResponse<T>(List<T> items, int page, int size, long totalItems, int totalPages) {
    public static <T> PageResponse<T> fromPage(List<T> items, int requestedPage, int requestedSize, long totalItems) {
        int page = Math.max(0, requestedPage);
        int size = Math.max(1, Math.min(requestedSize, 200));
        int pages = totalItems == 0 ? 0 : (int) Math.ceil(totalItems / (double) size);
        return new PageResponse<>(List.copyOf(items), page, size, totalItems, pages);
    }

    public static <T> PageResponse<T> of(List<T> source, int requestedPage, int requestedSize) {
        int page = Math.max(0, requestedPage);
        int size = Math.max(1, Math.min(requestedSize, 200));
        int from = Math.min(page * size, source.size());
        int to = Math.min(from + size, source.size());
        int pages = source.isEmpty() ? 0 : (int) Math.ceil(source.size() / (double) size);
        return new PageResponse<>(List.copyOf(source.subList(from, to)), page, size, source.size(), pages);
    }
}
