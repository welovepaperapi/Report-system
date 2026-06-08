package me.welovepaperapi.reports.api.model;

import me.welovepaperapi.reports.api.enums.ReportStatus;
import me.welovepaperapi.reports.api.enums.ReportTemplate;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

public interface Report {
    String getId();
    UUID getReporter();
    UUID getReported();
    String getReporterName();
    String getReportedName();
    ReportTemplate getTemplate();
    @Nullable String getCustomReason();
    ReportStatus getStatus();
    @Nullable UUID getHandledBy();
    @Nullable String getHandledByName();
    @Nullable String getModNote();
    String getServer();
    Instant getCreatedAt();
    @Nullable Instant getHandledAt();

    default String getReason() {
        if (getTemplate() == ReportTemplate.OTHER && getCustomReason() != null && !getCustomReason().isBlank()) {
            return getCustomReason();
        }
        return getTemplate().getDisplayName();
    }
}
