package me.welovepaperapi.reports.bukkit.gui;

import me.welovepaperapi.reports.api.dto.ReportResponse;
import me.welovepaperapi.reports.api.enums.ReportStatus;
import me.welovepaperapi.reports.api.service.ReportService;
import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import net.kyori.adventure.text.Component;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;

import java.util.List;

public class ReportDetailGui implements InventoryProvider {

    private final ReportService reportService;
    private final String reportId;

    public ReportDetailGui(ReportService reportService, String reportId) {
        this.reportService = reportService;
        this.reportId = reportId;
    }

    public void open(Player player) {
        SmartInventory.builder()
            .id("reportDetail")
            .provider(this)
            .size(4, 9)
            .title("§8Report §7#" + reportId.substring(0, 8))
            .manager(SmartInventoryManager.getManager())
            .build()
            .open(player);
    }

    @Override
    public void init(Player player, InventoryContents contents) {
        fillBorder(contents);

        reportService.getReport(reportId).thenAccept(opt -> {
            if (opt.isEmpty()) {
                player.closeInventory();
                player.sendMessage("§c§lReport › §cReport nicht gefunden.");
                return;
            }
            var r = opt.get();
            player.getServer().getScheduler().runTask(
                player.getServer().getPluginManager().getPlugin("ReportSystem"),
                () -> populateDetails(contents, player, r)
            );
        });
    }

    private void populateDetails(InventoryContents contents, Player player,
                                  ReportResponse r) {
        var statusName = switch (r.status()) {
            case OPEN -> "§c§lOffen";
            case RESOLVED -> "§a§lBerechtigt";
            case REJECTED -> "§8§lAbgelehnt";
        };

        contents.set(0, 4, ClickableItem.empty(createItem(
            getStatusMaterial(r.status()),
            "§8[ " + statusName + " §8]",
            List.of("§7Status: §f" + r.status().name())
        )));

        contents.set(1, 2, ClickableItem.empty(createItem(
            Material.PLAYER_HEAD,
            "§eGemeldeter",
            List.of("§7Name: §f" + r.reportedName(), "§7UUID: §7" + r.reported())
        )));

        contents.set(1, 4, ClickableItem.empty(createItem(
            Material.PLAYER_HEAD,
            "§eReporter",
            List.of("§7Name: §f" + r.reporterName(), "§7UUID: §7" + r.reporter())
        )));

        contents.set(1, 6, ClickableItem.empty(createItem(
            Material.PAPER,
            "§eGrund",
            List.of("§f" + r.reason(), "", "§7Template: §f" + r.template().getDisplayName())
        )));

        contents.set(2, 2, ClickableItem.empty(createItem(
            Material.COMPASS,
            "§eServer",
            List.of("§f" + r.server())
        )));

        contents.set(2, 4, ClickableItem.empty(createItem(
            Material.CLOCK,
            "§eErstellt",
            List.of("§f" + r.createdAt().toString().substring(0, 19))
        )));

        if (r.modNote() != null) {
            contents.set(2, 6, ClickableItem.empty(createItem(
                Material.BOOK,
                "§eNotiz",
                List.of("§f" + r.modNote())
            )));
        }

        if (r.handledByName() != null) {
            contents.set(2, 7, ClickableItem.empty(createItem(
                Material.ENDER_EYE,
                "§eBearbeiter",
                List.of("§f" + r.handledByName())
            )));
        }

        if (r.status() == ReportStatus.OPEN) {
            contents.set(3, 2, ClickableItem.of(
                createEnchantedItem(Material.EMERALD_BLOCK, "§a§l✔ Berechtigt"),
                e -> {
                    reportService.resolveReport(r.id(), e.getWhoClicked().getUniqueId(), e.getWhoClicked().getName())
                        .thenAccept(response -> {
                            e.getWhoClicked().sendMessage("§a§lReport › §aReport als berechtigt eingestuft.");
                            e.getWhoClicked().closeInventory();
                        })
                        .exceptionally(ex -> {
                            e.getWhoClicked().sendMessage("§c§lReport › §c" + ex.getCause().getMessage());
                            return null;
                        });
                }
            ));

            contents.set(3, 4, ClickableItem.of(
                createEnchantedItem(Material.REDSTONE_BLOCK, "§c§l✖ Ablehnen (Chat)"),
                e -> {
                    var admin = (Player) e.getWhoClicked();
                    admin.closeInventory();
                    ChatInputListener.waitForInput(admin, reason -> {
                        reportService.rejectReport(r.id(), admin.getUniqueId(), admin.getName(), reason)
                            .thenAccept(response ->
                                admin.sendMessage("§a§lReport › §aReport abgelehnt. Grund: §f" + reason)
                            )
                            .exceptionally(ex -> {
                                admin.sendMessage("§c§lReport › §c" + ex.getCause().getMessage());
                                return null;
                            });
                    });
                    admin.sendMessage("§e§lReport › §eGib den Ablehnungsgrund im Chat ein:");
                }
            ));
        }

        contents.set(3, 8, ClickableItem.of(
            createEnchantedItem(Material.ARROW, "§e§l← Zurück"),
            e -> new ReportAdminGui(reportService).open((Player) e.getWhoClicked())
        ));
    }

    @Override
    public void update(Player player, InventoryContents contents) {}

    private Material getStatusMaterial(ReportStatus status) {
        return switch (status) {
            case OPEN -> Material.REDSTONE_BLOCK;
            case RESOLVED -> Material.EMERALD_BLOCK;
            case REJECTED -> Material.BARRIER;
        };
    }

    private void fillBorder(InventoryContents contents) {
        var border = createItem(Material.GRAY_STAINED_GLASS_PANE, "§r", List.of());
        for (int col = 0; col < 9; col++) {
            contents.set(0, col, ClickableItem.empty(border));
            contents.set(3, col, ClickableItem.empty(border));
        }
        for (int row = 1; row < 3; row++) {
            contents.set(row, 0, ClickableItem.empty(border));
            contents.set(row, 8, ClickableItem.empty(border));
        }
    }

    private ItemStack createEnchantedItem(Material material, String name) {
        var item = new ItemStack(material);
        var meta = item.getItemMeta();
        meta.displayName(Component.text(name));
        var ench = Registry.ENCHANTMENT.get(NamespacedKey.minecraft("unbreaking"));
        if (ench != null) meta.addEnchant(ench, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createItem(Material material, String name, List<String> lore) {
        var item = new ItemStack(material);
        var meta = item.getItemMeta();
        meta.displayName(Component.text(name));
        meta.lore(lore.stream().map(Component::text).toList());
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }
}
