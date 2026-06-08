package me.welovepaperapi.reports.bukkit.service;

import me.welovepaperapi.reports.common.config.ReportConfig;
import me.welovepaperapi.reports.api.dto.ReportCreateRequest;
import me.welovepaperapi.reports.api.enums.ReportTemplate;
import me.welovepaperapi.reports.api.service.ReportService;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.geysermc.cumulus.form.SimpleForm;
import org.geysermc.cumulus.form.CustomForm;
import org.geysermc.floodgate.api.FloodgateApi;
import org.bukkit.plugin.Plugin;

import java.util.UUID;

public class BedrockFormService {

    private final ReportService reportService;
    private final ReportConfig config;
    private final boolean floodgateAvailable;

    public BedrockFormService(ReportService reportService, ReportConfig config, Plugin plugin) {
        this.reportService = reportService;
        this.config = config;
        this.floodgateAvailable = Bukkit.getPluginManager().getPlugin("floodgate") != null;
    }

    public boolean isBedrockPlayer(Player player) {
        if (!floodgateAvailable) return false;
        try {
            return FloodgateApi.getInstance().isFloodgatePlayer(player.getUniqueId());
        } catch (Exception e) {
            return false;
        }
    }

    public void openPlayerSelector(Player player) {
        if (!floodgateAvailable) return;

        var builder = SimpleForm.builder()
            .title("§8Spieler melden")
            .content("Wähle einen Spieler aus der Liste:");

        for (var target : Bukkit.getOnlinePlayers()) {
            if (target.equals(player)) continue;
            builder.button(target.getName());
        }

        builder.validResultHandler(response -> {
            int clicked = response.getClickedButtonId();
            int index = 0;
            for (var target : Bukkit.getOnlinePlayers()) {
                if (target.equals(player)) continue;
                if (index == clicked) {
                    openTemplateSelection(player, target.getUniqueId(), target.getName());
                    return;
                }
                index++;
            }
        });

        builder.closedOrInvalidResultHandler(() -> {});

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), builder.build());
    }

    public void openTemplateSelection(Player player, UUID targetUuid, String targetName) {
        if (!floodgateAvailable) return;

        var builder = SimpleForm.builder()
            .title("§8Grund auswählen")
            .content("Wähle einen Grund für die Meldung von §e" + targetName + "§r aus:");

        for (var template : ReportTemplate.values()) {
            builder.button(template.getDisplayName() + "\n" + template.getDescription());
        }

        builder.validResultHandler(response -> {
            int clicked = response.getClickedButtonId();
            var templates = ReportTemplate.values();
            if (clicked < 0 || clicked >= templates.length) return;

            var selected = templates[clicked];

            if (selected == ReportTemplate.OTHER) {
                openCustomReasonForm(player, targetUuid, targetName);
            } else {
                submitReport(player, targetUuid, targetName, selected, null);
            }
        });

        builder.closedOrInvalidResultHandler(() -> {});

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), builder.build());
    }

    private void openCustomReasonForm(Player player, UUID targetUuid, String targetName) {
        var form = CustomForm.builder()
            .title("§8Sonstiger Grund")
            .input("Gib bitte den genauen Grund für die Meldung von §e" + targetName + "§r an:")
            .validResultHandler(response -> {
                String customReason = response.asInput(0);
                if (customReason == null || customReason.isBlank()) {
                    player.sendMessage("§cBitte gib einen Grund an.");
                    return;
                }
                submitReport(player, targetUuid, targetName, ReportTemplate.OTHER, customReason);
            })
            .closedOrInvalidResultHandler(() -> {})
            .build();

        FloodgateApi.getInstance().sendForm(player.getUniqueId(), form);
    }

    private void submitReport(Player player, UUID targetUuid, String targetName,
                               ReportTemplate template, String customReason) {
        var request = new ReportCreateRequest(
            player.getUniqueId(),
            targetUuid,
            player.getName(),
            targetName,
            template,
            customReason,
            config.serverName()
        );

        reportService.createReport(request).thenAccept(response -> {
            player.sendMessage("§aDein Report gegen §6" + targetName + " §awurde aufgenommen (ID: §e" + response.id() + "§a).");
        }).exceptionally(ex -> {
            player.sendMessage("§cFehler: " + ex.getMessage());
            return null;
        });
    }
}
