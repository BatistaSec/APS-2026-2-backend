package com.app.model.dto.response;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;


public record ProblemDetails(
        String type,
        String title,
        int status,
        String detail,
        String instance,
        String timestamp
) {

    private static final DateTimeFormatter ISO_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    public static ProblemDetails of(String type, String title, int status, String detail, String instance) {
        String ts = LocalDateTime.now().format(ISO_FORMATTER);
        String resolvedType = (type != null && !type.isBlank()) ? type : "about:blank";
        return new ProblemDetails(resolvedType, title, status, detail, instance != null ? instance : "/", ts);
    }

    public String toJson() {
        return String.format(
                "{\"type\":\"%s\",\"title\":\"%s\",\"status\":%d,\"detail\":\"%s\",\"instance\":\"%s\",\"timestamp\":\"%s\"}",
                escape(type),
                escape(title),
                status,
                escape(detail),
                escape(instance),
                timestamp
        );
    }

    private static String escape(String s) {
        if (s == null) return "";
        return s.replace("\"", "\\\"").replace("\n", " ").replace("\r", "");
    }
}
