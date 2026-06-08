package me.welovepaperapi.reports.api.dto;

import me.welovepaperapi.reports.api.enums.ReportStatus;
import me.welovepaperapi.reports.api.enums.ReportTemplate;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

public record ReportResponse(
    String id,
    UUID reporter,
    UUID reported,
    String reporterName,
    String reportedName,
    ReportTemplate template,
    @Nullable String customReason,
    ReportStatus status,
    @Nullable UUID handledBy,
    @Nullable String handledByName,
    @Nullable String modNote,
    String server,
    Instant createdAt,
    @Nullable Instant handledAt
) {
    public String reason() {
        if (template == ReportTemplate.OTHER && customReason != null && !customReason.isBlank()) {
            return customReason;
        }
        return template.getDisplayName();
    }
}
