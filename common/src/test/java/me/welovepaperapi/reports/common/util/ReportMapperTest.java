package me.welovepaperapi.reports.common.util;

import me.welovepaperapi.reports.api.dto.ReportResponse;
import me.welovepaperapi.reports.api.enums.ReportStatus;
import me.welovepaperapi.reports.api.enums.ReportTemplate;
import me.welovepaperapi.reports.common.database.ReportModel;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ReportMapperTest {

    @Test
    void toResponse_shouldMapAllFields() {
        var id = ObjectId.get();
        var reporter = UUID.randomUUID();
        var reported = UUID.randomUUID();
        var now = Instant.now();

        var model = new ReportModel();
        model.setId(id);
        model.setReporter(reporter);
        model.setReported(reported);
        model.setReporterName("ReporterA");
        model.setReportedName("ReportedB");
        model.setTemplate(ReportTemplate.CHEATING.name());
        model.setCustomReason(null);
        model.setStatus(ReportStatus.OPEN);
        model.setServer("survival-1");
        model.setCreatedAt(now);
        model.setHandledAt(null);

        var response = ReportMapper.toResponse(model);

        assertEquals(id.toHexString(), response.id());
        assertEquals(reporter, response.reporter());
        assertEquals(reported, response.reported());
        assertEquals("ReporterA", response.reporterName());
        assertEquals("ReportedB", response.reportedName());
        assertEquals(ReportTemplate.CHEATING, response.template());
        assertNull(response.customReason());
        assertEquals(ReportStatus.OPEN, response.status());
        assertEquals("survival-1", response.server());
        assertEquals(now, response.createdAt());
        assertNull(response.handledAt());
    }

    @Test
    void toResponse_shouldMapHandledFields() {
        var id = ObjectId.get();
        var handledBy = UUID.randomUUID();
        var now = Instant.now();

        var model = new ReportModel();
        model.setId(id);
        model.setReporter(UUID.randomUUID());
        model.setReported(UUID.randomUUID());
        model.setReporterName("ReporterA");
        model.setReportedName("ReportedB");
        model.setTemplate(ReportTemplate.OTHER.name());
        model.setCustomReason("Custom reason text");
        model.setStatus(ReportStatus.RESOLVED);
        model.setHandledBy(handledBy);
        model.setHandledByName("ModeratorM");
        model.setModNote("Valid report");
        model.setServer("lobby-1");
        model.setCreatedAt(Instant.now());
        model.setHandledAt(now);

        var response = ReportMapper.toResponse(model);

        assertEquals(ReportStatus.RESOLVED, response.status());
        assertEquals(handledBy, response.handledBy());
        assertEquals("ModeratorM", response.handledByName());
        assertEquals("Valid report", response.modNote());
        assertEquals("Custom reason text", response.customReason());
        assertEquals(now, response.handledAt());
    }

    @Test
    void toModel_shouldCreateModelFromResponse() {
        var response = new ReportResponse(
            ObjectId.get().toHexString(),
            UUID.randomUUID(),
            UUID.randomUUID(),
            "ReporterA",
            "ReportedB",
            ReportTemplate.SPAM,
            null,
            ReportStatus.OPEN,
            null,
            null,
            null,
            "server-1",
            Instant.now(),
            null
        );

        var model = ReportMapper.toModel(response);

        assertEquals(response.reporter(), model.getReporter());
        assertEquals(response.reported(), model.getReported());
        assertEquals(response.reporterName(), model.getReporterName());
        assertEquals(response.reportedName(), model.getReportedName());
        assertEquals(response.template().name(), model.getTemplate());
        assertEquals(response.status(), model.getStatus());
    }
}
