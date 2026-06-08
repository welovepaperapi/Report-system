package me.welovepaperapi.reports.common.database;

import me.welovepaperapi.reports.api.enums.ReportStatus;
import me.welovepaperapi.reports.api.enums.ReportTemplate;
import dev.morphia.annotations.Entity;
import dev.morphia.annotations.Id;
import dev.morphia.annotations.Indexed;
import org.bson.types.ObjectId;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.UUID;

@Entity("reports")
public class ReportModel {
    @Id
    private ObjectId id;
    @Indexed
    private UUID reporter;
    @Indexed
    private UUID reported;
    private String reporterName;
    private String reportedName;
    private String template;
    @Nullable private String customReason;
    @Indexed
    private ReportStatus status;
    @Nullable private UUID handledBy;
    @Nullable private String handledByName;
    @Nullable private String modNote;
    @Indexed
    private String server;
    @Indexed
    private Instant createdAt;
    @Nullable private Instant handledAt;

    public ReportModel() {}

    public ReportModel(
        UUID reporter, UUID reported, String reporterName, String reportedName,
        ReportTemplate template, @Nullable String customReason, String server
    ) {
        this.reporter = reporter;
        this.reported = reported;
        this.reporterName = reporterName;
        this.reportedName = reportedName;
        this.template = template.name();
        this.customReason = customReason;
        this.server = server;
        this.status = ReportStatus.OPEN;
        this.createdAt = Instant.now();
    }

    public ObjectId getId() { return id; }
    public void setId(ObjectId id) { this.id = id; }

    public UUID getReporter() { return reporter; }
    public void setReporter(UUID reporter) { this.reporter = reporter; }

    public UUID getReported() { return reported; }
    public void setReported(UUID reported) { this.reported = reported; }

    public String getReporterName() { return reporterName; }
    public void setReporterName(String reporterName) { this.reporterName = reporterName; }

    public String getReportedName() { return reportedName; }
    public void setReportedName(String reportedName) { this.reportedName = reportedName; }

    public String getTemplate() { return template; }
    public void setTemplate(String template) { this.template = template; }

    @Nullable public String getCustomReason() { return customReason; }
    public void setCustomReason(@Nullable String customReason) { this.customReason = customReason; }

    public ReportStatus getStatus() { return status; }
    public void setStatus(ReportStatus status) { this.status = status; }

    @Nullable public UUID getHandledBy() { return handledBy; }
    public void setHandledBy(@Nullable UUID handledBy) { this.handledBy = handledBy; }

    @Nullable public String getHandledByName() { return handledByName; }
    public void setHandledByName(@Nullable String handledByName) { this.handledByName = handledByName; }

    @Nullable public String getModNote() { return modNote; }
    public void setModNote(@Nullable String modNote) { this.modNote = modNote; }

    public String getServer() { return server; }
    public void setServer(String server) { this.server = server; }

    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }

    @Nullable public Instant getHandledAt() { return handledAt; }
    public void setHandledAt(@Nullable Instant handledAt) { this.handledAt = handledAt; }
}
