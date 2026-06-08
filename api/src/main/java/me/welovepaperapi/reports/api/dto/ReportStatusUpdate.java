package me.welovepaperapi.reports.api.dto;

import me.welovepaperapi.reports.api.enums.ReportStatus;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

public record ReportStatusUpdate(
    String reportId,
    UUID reporterUuid,
    String reporterName,
    String reportedName,
    ReportStatus status,
    String handledBy,
    @Nullable String modNote,
    Instant timestamp
) {
}
