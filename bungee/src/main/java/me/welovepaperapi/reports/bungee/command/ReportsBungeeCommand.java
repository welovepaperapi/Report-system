package me.welovepaperapi.reports.bungee.command;

import me.welovepaperapi.reports.api.service.ReportService;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;

public class ReportsBungeeCommand extends Command {

    private static final String PERMISSION_ADMIN = "reports.admin";

    private final ReportService reportService;

    public ReportsBungeeCommand(ReportService reportService) {
        super("reports", PERMISSION_ADMIN);
        this.reportService = reportService;
    }

    @Override
    public void execute(CommandSender sender, String[] args) {
        if (!(sender instanceof ProxiedPlayer)) {
            sender.sendMessage(new TextComponent("§cNur für Spieler."));
            return;
        }

        sender.sendMessage(new TextComponent("§eBitte wechsle zu einem Server mit dem ReportSystem-Plugin."));
        sender.sendMessage(new TextComponent("§7Verwende /reports dort, um Reports zu verwalten."));
    }
}
