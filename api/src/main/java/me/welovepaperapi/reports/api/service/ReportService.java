package me.welovepaperapi.reports.api.service;

import me.welovepaperapi.reports.api.dto.ReportCreateRequest;
import me.welovepaperapi.reports.api.dto.ReportResponse;
import me.welovepaperapi.reports.api.dto.StatsResponse;
import me.welovepaperapi.reports.api.enums.ReportStatus;
import me.welovepaperapi.reports.api.enums.ReportTemplate;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public interface ReportService {
    CompletableFuture<ReportResponse> createReport(ReportCreateRequest request);
    CompletableFuture<Optional<ReportResponse>> getReport(String id);
    CompletableFuture<List<ReportResponse>> getReports(
        @Nullable ReportStatus status,
        @Nullable ReportTemplate template,
        @Nullable String server,
        @Nullable Instant since,
        int page,
        int size
    );
    CompletableFuture<ReportResponse> resolveReport(String id, UUID handledBy, String handledByName);
    CompletableFuture<ReportResponse> rejectReport(String id, UUID handledBy, String handledByName, String reason);
    CompletableFuture<StatsResponse> getStats();
    CompletableFuture<StatsResponse> getPlayerStats(UUID player);
    CompletableFuture<Long> getOpenReportCount();
}
