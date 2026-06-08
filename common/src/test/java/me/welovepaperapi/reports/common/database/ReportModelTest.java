package me.welovepaperapi.reports.common.database;

import me.welovepaperapi.reports.api.enums.ReportStatus;
import me.welovepaperapi.reports.api.enums.ReportTemplate;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

class ReportModelTest {

    @Test
    void noArgConstructor_shouldCreateEmptyModel() {
        var model = new ReportModel();
        assertNull(model.getId());
        assertNull(model.getReporter());
        assertNull(model.getStatus());
    }

    @Test
    void parameterizedConstructor_shouldSetFields() {
        var reporter = UUID.randomUUID();
        var reported = UUID.randomUUID();

        var model = new ReportModel(
            reporter, reported, "ReporterA", "ReportedB",
            ReportTemplate.GRIEFING, "Broke my house", "survival-1"
        );

        assertEquals(reporter, model.getReporter());
        assertEquals(reported, model.getReported());
        assertEquals("ReporterA", model.getReporterName());
        assertEquals("ReportedB", model.getReportedName());
        assertEquals(ReportTemplate.GRIEFING.name(), model.getTemplate());
        assertEquals("Broke my house", model.getCustomReason());
        assertEquals(ReportStatus.OPEN, model.getStatus());
        assertEquals("survival-1", model.getServer());
        assertNotNull(model.getCreatedAt());
        assertNull(model.getId());
        assertNull(model.getHandledBy());
        assertNull(model.getHandledAt());
    }

    @Test
    void setters_shouldUpdateValues() {
        var model = new ReportModel();

        var id = new org.bson.types.ObjectId();
        var now = Instant.now();
        var handledBy = UUID.randomUUID();

        model.setId(id);
        model.setStatus(ReportStatus.RESOLVED);
        model.setHandledBy(handledBy);
        model.setHandledByName("ModeratorM");
        model.setModNote("Accepted");
        model.setHandledAt(now);

        assertEquals(id, model.getId());
        assertEquals(ReportStatus.RESOLVED, model.getStatus());
        assertEquals(handledBy, model.getHandledBy());
        assertEquals("ModeratorM", model.getHandledByName());
        assertEquals("Accepted", model.getModNote());
        assertEquals(now, model.getHandledAt());
    }
}
