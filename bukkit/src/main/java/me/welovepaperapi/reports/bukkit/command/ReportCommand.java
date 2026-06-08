package me.welovepaperapi.reports.bukkit.command;

import me.welovepaperapi.reports.common.config.ReportConfig;
import me.welovepaperapi.reports.common.util.UuidResolver;
import me.welovepaperapi.reports.api.service.ReportService;
import me.welovepaperapi.reports.bukkit.gui.ReportPlayerSelectorGui;
import me.welovepaperapi.reports.bukkit.gui.ReportTemplateGui;
import me.welovepaperapi.reports.bukkit.service.BedrockFormService;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class ReportCommand implements CommandExecutor {

    private static final String PERMISSION_USE = "reports.use";
    private static final String PERMISSION_ADMIN = "reports.admin";

    private final ReportService reportService;
    private final ReportConfig config;
    private final BedrockFormService bedrockFormService;
    private final UuidResolver uuidResolver;
    private final Plugin plugin;

    public ReportCommand(ReportService reportService, ReportConfig config, BedrockFormService bedrockFormService, Plugin plugin) {
        this.reportService = reportService;
        this.config = config;
        this.bedrockFormService = bedrockFormService;
        this.uuidResolver = new UuidResolver(config.bedrockPrefix());
        this.plugin = plugin;
    }

    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        plugin.getLogger().info("Command /" + label + " executed by " + sender.getName()
            + " with args: " + Arrays.toString(args));

        if (!(sender instanceof Player player)) {
            sender.sendMessage("§cDieser Befehl kann nur von Spielern ausgeführt werden.");
            return true;
        }

        if (!player.hasPermission(PERMISSION_USE)) {
            player.sendMessage("§cDu hast keine Berechtigung für diesen Befehl.");
            plugin.getLogger().info("Player " + player.getName() + " lacks permission " + PERMISSION_USE);
            return true;
        }

        if (args.length == 0) {
            openPlayerSelector(player);
            return true;
        }

        var subCommand = args[0].toLowerCase();

        switch (subCommand) {
            case "resolve" -> handleResolve(player, args);
            case "reject" -> handleReject(player, args);
            case "details" -> handleDetails(player, args);
            case "stats" -> handleStats(player, args);
            default -> handlePlayerReport(player, args[0]);
        }

        return true;
    }

    private void openPlayerSelector(Player player) {
        if (bedrockFormService != null && bedrockFormService.isBedrockPlayer(player)) {
            bedrockFormService.openPlayerSelector(player);
        } else {
            new ReportPlayerSelectorGui(reportService, config).open(player);
        }
    }

    private void handlePlayerReport(Player player, String targetName) {
        var onlineTarget = player.getServer().getPlayer(targetName);
        if (onlineTarget != null) {
            openTemplateSelection(player, onlineTarget.getUniqueId(), onlineTarget.getName());
            return;
        }

        player.sendMessage("§eSuche Spieler §6" + targetName + "§e...");

        uuidResolver.resolve(targetName).thenAccept(resolved -> {
            if (resolved.isEmpty()) {
                player.sendMessage("§cSpieler §6" + targetName + " §cwurde nicht gefunden.");
                return;
            }
            var r = resolved.get();
            plugin.getServer().getScheduler().runTask(plugin,
                () -> openTemplateSelection(player, r.uuid(), r.name())
            );
        }).exceptionally(ex -> {
            player.sendMessage("§cFehler bei der Spielersuche: " + ex.getMessage());
            return null;
        });
    }

    private void openTemplateSelection(Player player, UUID targetUuid, String targetName) {
        if (bedrockFormService != null && bedrockFormService.isBedrockPlayer(player)) {
            bedrockFormService.openTemplateSelection(player, targetUuid, targetName);
        } else {
            new ReportTemplateGui(reportService, config, targetUuid, targetName).open(player);
        }
    }

    private void handleResolve(Player player, String[] args) {
        if (!player.isOp() && !player.hasPermission(PERMISSION_ADMIN)) {
            player.sendMessage("§cKeine Berechtigung.");
            return;
        }
        if (args.length < 2) {
            player.sendMessage("§cVerwendung: /report resolve <id>");
            return;
        }
        reportService.resolveReport(args[1], player.getUniqueId(), player.getName())
            .thenAccept(response ->
                player.sendMessage("§aReport §6" + response.id() + " §awurde als berechtigt eingestuft.")
            )
            .exceptionally(ex -> {
                player.sendMessage("§c" + ex.getCause().getMessage());
                return null;
            });
    }

    private void handleReject(Player player, String[] args) {
        if (!player.isOp() && !player.hasPermission(PERMISSION_ADMIN)) {
            player.sendMessage("§cKeine Berechtigung.");
            return;
        }
        if (args.length < 3) {
            player.sendMessage("§cVerwendung: /report reject <id> <grund>");
            return;
        }
        var reason = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
        reportService.rejectReport(args[1], player.getUniqueId(), player.getName(), reason)
            .thenAccept(response ->
                player.sendMessage("§aReport §6" + response.id() + " §awurde abgelehnt.")
            )
            .exceptionally(ex -> {
                player.sendMessage("§c" + ex.getCause().getMessage());
                return null;
            });
    }

    private void handleDetails(Player player, String[] args) {
        if (!player.isOp() && !player.hasPermission(PERMISSION_ADMIN)) {
            player.sendMessage("§cKeine Berechtigung.");
            return;
        }
        if (args.length < 2) {
            player.sendMessage("§cVerwendung: /report details <id>");
            return;
        }
        reportService.getReport(args[1]).thenAccept(opt -> {
            if (opt.isEmpty()) {
                player.sendMessage("§cReport nicht gefunden.");
                return;
            }
            var r = opt.get();
            player.sendMessage("§8§m§l----------------------------------------");
            player.sendMessage("§6Report §e#" + r.id());
            player.sendMessage("§7Gemeldeter: §f" + r.reportedName());
            player.sendMessage("§7Reporter: §f" + r.reporterName());
            player.sendMessage("§7Grund: §f" + r.reason());
            player.sendMessage("§7Status: §f" + r.status().name());
            player.sendMessage("§7Server: §f" + r.server());
            player.sendMessage("§7Erstellt: §f" + r.createdAt());
            if (r.modNote() != null) {
                player.sendMessage("§7Mod-Notiz: §f" + r.modNote());
            }
            if (r.handledByName() != null) {
                player.sendMessage("§7Bearbeitet von: §f" + r.handledByName());
            }
            player.sendMessage("§8§m§l----------------------------------------");
        });
    }

    private void handleStats(Player player, String[] args) {
        if (!player.isOp() && !player.hasPermission(PERMISSION_ADMIN)) {
            player.sendMessage("§cKeine Berechtigung.");
            return;
        }
        var statsFuture = args.length >= 2
            ? uuidResolver.resolve(args[1]).thenCompose(resolved ->
                resolved.map(r -> reportService.getPlayerStats(r.uuid()))
                    .orElseGet(() -> CompletableFuture.completedFuture(null)))
            : reportService.getStats();

        statsFuture.thenAccept(stats -> {
            if (stats == null) {
                player.sendMessage("§cSpieler nicht gefunden.");
                return;
            }
            player.sendMessage("§8§m§l----------------------------------------");
            player.sendMessage("§6Report-Statistiken");
            if (args.length >= 2) player.sendMessage("§7Spieler: §f" + args[1]);
            player.sendMessage("§7Gesamt: §f" + stats.totalReports());
            player.sendMessage("§aOffen: §f" + stats.openReports());
            player.sendMessage("§bBerechtigt: §f" + stats.resolvedReports());
            player.sendMessage("§cAbgelehnt: §f" + stats.rejectedReports());
            player.sendMessage("§8§m§l----------------------------------------");
        });
    }
}
