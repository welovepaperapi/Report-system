package me.welovepaperapi.reports.api.dto;

import me.welovepaperapi.reports.api.enums.ReportTemplate;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record ReportCreateRequest(
    UUID reporter,
    UUID reported,
    String reporterName,
    String reportedName,
    ReportTemplate template,
    @Nullable String customReason,
    String server
) {
    public String reason() {
        if (template == ReportTemplate.OTHER && customReason != null && !customReason.isBlank()) {
            return customReason;
        }
        return template.getDisplayName();
    }
}
