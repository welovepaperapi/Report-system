package me.welovepaperapi.reports.bukkit.command;

import me.welovepaperapi.reports.api.service.ReportService;
import me.welovepaperapi.reports.bukkit.gui.ReportAdminGui;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.logging.Logger;

public class ReportsCommand implements CommandExecutor {

    private static final String PERMISSION_ADMIN = "reports.admin";

    private final ReportService reportService;
    private final Logger logger;

    public ReportsCommand(ReportService reportService, Plugin plugin) {
        this.reportService = reportService;
        this.logger = plugin.getLogger();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        logger.info("Command /" + label + " executed by " + sender.getName());

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cDieser Befehl kann nur von Spielern ausgeführt werden.");
            return true;
        }

        if (!player.isOp() && !player.hasPermission(PERMISSION_ADMIN)) {
            player.sendMessage("§cDu hast keine Berechtigung für diesen Befehl.");
            logger.info("Player " + player.getName() + " lacks permission " + PERMISSION_ADMIN
                + " (isOp=" + player.isOp() + ")");
            return true;
        }

        new ReportAdminGui(reportService).open(player);
        return true;
    }
}
