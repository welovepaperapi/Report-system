package me.welovepaperapi.reports.common.service;

import me.welovepaperapi.reports.api.dto.ReportCreateRequest;
import me.welovepaperapi.reports.api.dto.ReportNewNotification;
import me.welovepaperapi.reports.api.dto.ReportResponse;
import me.welovepaperapi.reports.api.dto.ReportStatusUpdate;
import me.welovepaperapi.reports.api.dto.StatsResponse;
import me.welovepaperapi.reports.api.enums.ReportStatus;
import me.welovepaperapi.reports.api.enums.ReportTemplate;
import me.welovepaperapi.reports.common.database.ReportModel;
import me.welovepaperapi.reports.common.database.ReportRepository;
import me.welovepaperapi.reports.common.messaging.RedisPublisher;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.*;

class ReportServiceImplTest {

    private ReportRepository repository;
    private RedisPublisher publisher;
    private ReportServiceImpl service;

    @BeforeEach
    void setUp() {
        repository = new FakeRepository();
        publisher = new RedisPublisher(null) {
            @Override public void publishNewReport(ReportNewNotification notification) {}
            @Override public void publishStatusUpdate(ReportStatusUpdate update) {}
        };
        service = new ReportServiceImpl(repository, publisher, "test-server");
    }

    @Test
    void createReport_shouldReturnResponse() {
        var request = new ReportCreateRequest(
            UUID.randomUUID(), UUID.randomUUID(),
            "ReporterA", "ReportedB",
            ReportTemplate.CHEATING, null, "test-server"
        );

        var response = service.createReport(request).join();

        assertNotNull(response.id());
        assertEquals("ReporterA", response.reporterName());
        assertEquals("ReportedB", response.reportedName());
        assertEquals(ReportTemplate.CHEATING, response.template());
        assertEquals(ReportStatus.OPEN, response.status());
        assertEquals("test-server", response.server());
    }

    @Test
    void getReport_found_shouldReturnReport() {
        var request = new ReportCreateRequest(
            UUID.randomUUID(), UUID.randomUUID(),
            "ReporterA", "ReportedB",
            ReportTemplate.INSULT, null, "test-server"
        );
        var created = service.createReport(request).join();

        var found = service.getReport(created.id()).join();

        assertTrue(found.isPresent());
        assertEquals(created.id(), found.get().id());
    }

    @Test
    void getReport_notFound_shouldReturnEmpty() {
        var result = service.getReport("000000000000000000000000").join();
        assertTrue(result.isEmpty());
    }

    @Test
    void resolveReport_shouldUpdateStatus() {
        var request = new ReportCreateRequest(
            UUID.randomUUID(), UUID.randomUUID(),
            "ReporterA", "ReportedB",
            ReportTemplate.BUGUSING, null, "test-server"
        );
        var created = service.createReport(request).join();
        var moderator = UUID.randomUUID();

        var resolved = service.resolveReport(created.id(), moderator, "ModeratorM").join();

        assertEquals(ReportStatus.RESOLVED, resolved.status());
        assertEquals(moderator, resolved.handledBy());
        assertEquals("ModeratorM", resolved.handledByName());
    }

    @Test
    void rejectReport_shouldUpdateStatusWithNote() {
        var request = new ReportCreateRequest(
            UUID.randomUUID(), UUID.randomUUID(),
            "ReporterA", "ReportedB",
            ReportTemplate.SPAM, null, "test-server"
        );
        var created = service.createReport(request).join();
        var moderator = UUID.randomUUID();

        var rejected = service.rejectReport(created.id(), moderator, "ModeratorM", "No evidence").join();

        assertEquals(ReportStatus.REJECTED, rejected.status());
        assertEquals("No evidence", rejected.modNote());
    }

    @Test
    void getStats_shouldReturnCounts() {
        var stats = service.getStats().join();
        assertNotNull(stats);
    }

    static class FakeRepository extends ReportRepository {
        private final java.util.Map<String, ReportModel> store = new java.util.concurrent.ConcurrentHashMap<>();

        FakeRepository() {
            super(null);
        }

        @Override
        public CompletableFuture<ReportResponse> insert(ReportModel report) {
            return CompletableFuture.supplyAsync(() -> {
                report.setId(new org.bson.types.ObjectId());
                if (report.getCreatedAt() == null) report.setCreatedAt(Instant.now());
                var response = toResponse(report);
                store.put(response.id(), report);
                return response;
            });
        }

        @Override
        public CompletableFuture<Optional<ReportResponse>> findById(String id) {
            var model = store.get(id);
            if (model == null) return CompletableFuture.completedFuture(Optional.empty());
            return CompletableFuture.completedFuture(Optional.of(toResponse(model)));
        }

        @Override
        public CompletableFuture<Void> updateStatus(
            String id, ReportStatus status, UUID handledBy, String handledByName, String modNote
        ) {
            var model = store.get(id);
            if (model == null) {
                return CompletableFuture.failedFuture(new IllegalArgumentException("Not found"));
            }
            model.setStatus(status);
            model.setHandledBy(handledBy);
            model.setHandledByName(handledByName);
            if (modNote != null) model.setModNote(modNote);
            model.setHandledAt(Instant.now());
            return CompletableFuture.completedFuture(null);
        }

        @Override
        public CompletableFuture<StatsResponse> getStats() {
            long open = store.values().stream().filter(m -> m.getStatus() == ReportStatus.OPEN).count();
            long resolved = store.values().stream().filter(m -> m.getStatus() == ReportStatus.RESOLVED).count();
            long rejected = store.values().stream().filter(m -> m.getStatus() == ReportStatus.REJECTED).count();
            return CompletableFuture.completedFuture(
                new StatsResponse(store.size(), open, resolved, rejected)
            );
        }

        @Override
        public CompletableFuture<Long> getOpenReportCount() {
            long count = store.values().stream()
                .filter(m -> m.getStatus() == ReportStatus.OPEN)
                .count();
            return CompletableFuture.completedFuture(count);
        }

        private ReportResponse toResponse(ReportModel model) {
            return new ReportResponse(
                model.getId().toHexString(),
                model.getReporter(),
                model.getReported(),
                model.getReporterName(),
                model.getReportedName(),
                ReportTemplate.valueOf(model.getTemplate()),
                model.getCustomReason(),
                model.getStatus(),
                model.getHandledBy(),
                model.getHandledByName(),
                model.getModNote(),
                model.getServer(),
                model.getCreatedAt(),
                model.getHandledAt()
            );
        }
    }
}
