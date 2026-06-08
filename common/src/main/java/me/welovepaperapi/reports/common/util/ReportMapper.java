package me.welovepaperapi.reports.common.util;

import me.welovepaperapi.reports.api.dto.ReportResponse;
import me.welovepaperapi.reports.api.enums.ReportTemplate;
import me.welovepaperapi.reports.common.database.ReportModel;

public final class ReportMapper {

    private ReportMapper() {}

    public static ReportResponse toResponse(ReportModel model) {
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

    public static ReportModel toModel(ReportResponse response) {
        var model = new ReportModel();
        model.setReporter(response.reporter());
        model.setReported(response.reported());
        model.setReporterName(response.reporterName());
        model.setReportedName(response.reportedName());
        model.setTemplate(response.template().name());
        model.setCustomReason(response.customReason());
        model.setStatus(response.status());
        model.setHandledBy(response.handledBy());
        model.setHandledByName(response.handledByName());
        model.setModNote(response.modNote());
        model.setServer(response.server());
        model.setCreatedAt(response.createdAt());
        model.setHandledAt(response.handledAt());
        return model;
    }
}
