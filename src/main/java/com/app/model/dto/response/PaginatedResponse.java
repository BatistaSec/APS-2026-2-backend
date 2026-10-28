package com.app.model.dto.response;

import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;


public record PaginatedResponse(
        List<MedicaoResponse> data,
        int page,
        int pageSize,
        long totalItems,
        int totalPages,
        boolean hasNext,
        boolean hasPrevious
) {

    public static PaginatedResponse of(List<MedicaoResponse> data, int page, int pageSize, long totalItems) {
        int safePage = Math.max(1, page);
        int safePageSize = Math.max(1, pageSize);
        int totalPages = safePageSize > 0 ? (int) Math.ceil((double) totalItems / safePageSize) : 1;
        if (totalPages == 0) totalPages = 1;
        boolean hasNext = safePage < totalPages;
        boolean hasPrevious = safePage > 1;

        return new PaginatedResponse(
                data != null ? data : List.of(),
                safePage,
                safePageSize,
                totalItems,
                totalPages,
                hasNext,
                hasPrevious
        );
    }

    public String toJson() {
        String dataJson = "[" + data.stream()
                .map(MedicaoResponse::toJson)
                .collect(Collectors.joining(",")) + "]";

        return String.format(Locale.US,
                "{" +
                "\"data\":%s," +
                "\"pagination\":{" +
                "\"page\":%d," +
                "\"pageSize\":%d," +
                "\"totalItems\":%d," +
                "\"totalPages\":%d," +
                "\"hasNext\":%b," +
                "\"hasPrevious\":%b" +
                "}" +
                "}",
                dataJson, page, pageSize, totalItems, totalPages, hasNext, hasPrevious
        );
    }
}
