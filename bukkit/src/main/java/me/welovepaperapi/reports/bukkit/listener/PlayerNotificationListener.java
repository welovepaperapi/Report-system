package me.welovepaperapi.reports.bukkit.listener;

import me.welovepaperapi.reports.api.dto.ReportNewNotification;
import org.bukkit.Bukkit;
import org.bukkit.plugin.Plugin;

public class PlayerNotificationListener {

    private static final String PERMISSION_ADMIN = "reports.admin";

    private final Plugin plugin;

    public PlayerNotificationListener(Plugin plugin) {
        this.plugin = plugin;
    }

    public void onNewReport(ReportNewNotification notification) {
        Bukkit.getScheduler().runTask(plugin, () -> {
            for (var player : Bukkit.getOnlinePlayers()) {
                if (player.hasPermission(PERMISSION_ADMIN)) {
                    player.sendMessage("§8[§cREPORT§8] §e" + notification.reported()
                        + " §7wurde gemeldet von §e" + notification.reporter()
                        + " §7(Grund: §f" + notification.reason() + "§7)");
                    player.sendMessage("§7Verwende §e/reports §7um offene Reports zu verwalten.");
                }
            }
        });
    }
}
