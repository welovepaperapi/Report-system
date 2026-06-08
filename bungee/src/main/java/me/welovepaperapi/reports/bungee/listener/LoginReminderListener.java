package me.welovepaperapi.reports.bungee.listener;

import me.welovepaperapi.reports.api.service.ReportService;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.event.PostLoginEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.event.EventHandler;

public class LoginReminderListener implements Listener {

    private static final String PERMISSION_ADMIN = "reports.admin";

    private final ReportService reportService;

    public LoginReminderListener(ReportService reportService) {
        this.reportService = reportService;
    }

    @EventHandler
    public void onPostLogin(PostLoginEvent event) {
        var player = event.getPlayer();

        if (!player.hasPermission(PERMISSION_ADMIN)) return;

        reportService.getOpenReportCount().thenAccept(count -> {
            if (count > 0) {
                player.sendMessage(new TextComponent(
                    "§8[§cREPORT§8] §eDu hast aktuell §6" + count + " §eoffene Reports."
                ));
                player.sendMessage(new TextComponent(
                    "§7Nutze §e/reports §7um diese zu verwalten."
                ));
            }
        }).exceptionally(ex -> {
            return null;
        });
    }
}
