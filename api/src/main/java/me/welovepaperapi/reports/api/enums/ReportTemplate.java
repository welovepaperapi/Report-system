package me.welovepaperapi.reports.api.enums;

public enum ReportTemplate {
    CHEATING("Cheating", "Verdacht auf Cheats/Hacks"),
    INSULT("Beleidigung", "Beleidigung / Toxisches Verhalten"),
    BUGUSING("Bug-Ausnutzung", "Ausnutzen von Bugs/Glitches"),
    GRIEFING("Griefing", "Zerstörung von Bauwerken"),
    SPAM("Spam", "Chat-Spam / Werbung"),
    OTHER("Sonstiges", "Sonstiges");

    private final String displayName;
    private final String description;

    ReportTemplate(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }
}
