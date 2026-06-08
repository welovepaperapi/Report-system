package me.welovepaperapi.reports.bukkit.listener;

import me.welovepaperapi.reports.api.dto.ReportStatusUpdate;
import me.welovepaperapi.reports.api.enums.ReportStatus;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class StatusUpdateListener {

    private final Plugin plugin;

    public StatusUpdateListener(Plugin plugin) {
        this.plugin = plugin;
    }

    public void onStatusUpdate(ReportStatusUpdate update) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            var reporter = Bukkit.getPlayer(update.reporterUuid());
            if (reporter == null || !reporter.isOnline()) return;

            if (update.status() == ReportStatus.RESOLVED) {
                reporter.sendMessage("§8[§aREPORT§8] §aDein Report gegen §e" + update.reportedName()
                    + " §awurde bearbeitet und als berechtigt eingestuft.");
            } else if (update.status() == ReportStatus.REJECTED) {
                var reason = update.modNote() != null ? " Grund: §f" + update.modNote() : "";
                reporter.sendMessage("§8[§6REPORT§8] §eDein Report gegen §6" + update.reportedName()
                    + " §ewurde geprüft und abgelehnt." + reason);
            }
        });
    }
}
