package me.welovepaperapi.reports.api.dto;

import java.time.Instant;

public record ReportNewNotification(
    String reportId,
    String reporter,
    String reported,
    String reason,
    String server,
    Instant timestamp
) {
}
