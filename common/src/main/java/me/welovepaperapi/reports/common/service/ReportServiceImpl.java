package me.welovepaperapi.reports.common.service;

import me.welovepaperapi.reports.api.dto.*;
import me.welovepaperapi.reports.api.enums.ReportStatus;
import me.welovepaperapi.reports.api.enums.ReportTemplate;
import me.welovepaperapi.reports.api.service.ReportService;
import me.welovepaperapi.reports.common.database.ReportModel;
import me.welovepaperapi.reports.common.database.ReportRepository;
import me.welovepaperapi.reports.common.messaging.RedisPublisher;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ReportServiceImpl implements ReportService {

    private final ReportRepository repository;
    private final RedisPublisher publisher;
    private final String serverName;

    public ReportServiceImpl(ReportRepository repository, RedisPublisher publisher, String serverName) {
        this.repository = repository;
        this.publisher = publisher;
        this.serverName = serverName;
    }

    @Override
    public CompletableFuture<ReportResponse> createReport(ReportCreateRequest request) {
        var model = new ReportModel(
            request.reporter(), request.reported(),
            request.reporterName(), request.reportedName(),
            request.template(), request.customReason(),
            request.server()
        );

        return repository.insert(model).thenApply(response -> {
            var notification = new ReportNewNotification(
                response.id(),
                response.reporterName(),
                response.reportedName(),
                response.reason(),
                response.server(),
                response.createdAt()
            );
            publisher.publishNewReport(notification);
            return response;
        });
    }

    @Override
    public CompletableFuture<Optional<ReportResponse>> getReport(String id) {
        return repository.findById(id);
    }

    @Override
    public CompletableFuture<List<ReportResponse>> getReports(
        @Nullable ReportStatus status, @Nullable ReportTemplate template,
        @Nullable String server, @Nullable Instant since, int page, int size
    ) {
        return repository.findReports(status, template, server, since, page, size);
    }

    @Override
    public CompletableFuture<ReportResponse> resolveReport(String id, UUID handledBy, String handledByName) {
        return repository.findById(id).thenCompose(opt -> {
            if (opt.isEmpty()) {
                return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Report not found: " + id)
                );
            }
            var report = opt.get();
            if (report.status() != ReportStatus.OPEN) {
                return CompletableFuture.failedFuture(
                    new IllegalStateException("Report is already " + report.status().name().toLowerCase())
                );
            }
            return repository.updateStatus(id, ReportStatus.RESOLVED, handledBy, handledByName, null)
                .thenCompose(upd -> repository.findById(id))
                .thenApply(updated -> {
                    updated.ifPresent(response -> {
                        var statusUpdate = new ReportStatusUpdate(
                            response.id(), response.reporter(), response.reporterName(),
                            response.reportedName(), ReportStatus.RESOLVED,
                            handledByName, null, Instant.now()
                        );
                        publisher.publishStatusUpdate(statusUpdate);
                    });
                    return updated.orElseThrow();
                });
        });
    }

    @Override
    public CompletableFuture<ReportResponse> rejectReport(
        String id, UUID handledBy, String handledByName, String reason
    ) {
        return repository.findById(id).thenCompose(opt -> {
            if (opt.isEmpty()) {
                return CompletableFuture.failedFuture(
                    new IllegalArgumentException("Report not found: " + id)
                );
            }
            var report = opt.get();
            if (report.status() != ReportStatus.OPEN) {
                return CompletableFuture.failedFuture(
                    new IllegalStateException("Report is already " + report.status().name().toLowerCase())
                );
            }
            return repository.updateStatus(id, ReportStatus.REJECTED, handledBy, handledByName, reason)
                .thenCompose(upd -> repository.findById(id))
                .thenApply(updated -> {
                    updated.ifPresent(response -> {
                        var statusUpdate = new ReportStatusUpdate(
                            response.id(), response.reporter(), response.reporterName(),
                            response.reportedName(), ReportStatus.REJECTED,
                            handledByName, reason, Instant.now()
                        );
                        publisher.publishStatusUpdate(statusUpdate);
                    });
                    return updated.orElseThrow();
                });
        });
    }

    @Override
    public CompletableFuture<StatsResponse> getStats() {
        return repository.getStats();
    }

    @Override
    public CompletableFuture<StatsResponse> getPlayerStats(UUID player) {
        return repository.getPlayerStats(player);
    }

    @Override
    public CompletableFuture<Long> getOpenReportCount() {
        return repository.getOpenReportCount();
    }
}
