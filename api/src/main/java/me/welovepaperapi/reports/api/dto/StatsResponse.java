package me.welovepaperapi.reports.api.dto;

public record StatsResponse(
    long totalReports,
    long openReports,
    long resolvedReports,
    long rejectedReports
) {
}
