package me.welovepaperapi.reports.bukkit.gui;

import me.welovepaperapi.reports.api.dto.ReportResponse;
import me.welovepaperapi.reports.api.enums.ReportStatus;
import me.welovepaperapi.reports.api.enums.ReportTemplate;
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

import java.util.ArrayList;
import java.util.List;

public class ReportAdminGui implements InventoryProvider {

    private static final int INNER_COLS = 7;
    private static final int INNER_ROWS = 4;
    private static final int ITEMS_PER_PAGE = INNER_COLS * INNER_ROWS;

    private final ReportService reportService;
    private ReportStatus currentFilter;
    private ReportTemplate currentTemplateFilter;
    private int currentPage;

    public ReportAdminGui(ReportService reportService) {
        this.reportService = reportService;
        this.currentFilter = ReportStatus.OPEN;
        this.currentTemplateFilter = null;
        this.currentPage = 0;
    }

    public void open(Player player) {
        SmartInventory.builder()
            .id("reportAdmin")
            .provider(this)
            .size(6, 9)
            .title("§8§lReports verwalten")
            .manager(SmartInventoryManager.getManager())
            .build()
            .open(player);
    }

    @Override
    public void init(Player player, InventoryContents contents) {
        fillBorder(contents);

        contents.set(0, 4, ClickableItem.empty(createItem(
            Material.BOOK,
            "§b§lReport-Übersicht",
            List.of(
                "§7Filter: " + (currentFilter != null ? "§e" + currentFilter.name() : "§aAlle"),
                "§7Lade Daten..."
            )
        )));

        for (int row = 1; row <= INNER_ROWS; row++) {
            for (int col = 1; col <= INNER_COLS; col++) {
                contents.set(row, col, ClickableItem.empty(createItem(
                    Material.GRAY_STAINED_GLASS_PANE, "§r", List.of()
                )));
            }
        }

        reportService.getReports(currentFilter, currentTemplateFilter, null, null, currentPage, ITEMS_PER_PAGE)
            .thenAccept(reports -> {
                player.getServer().getScheduler().runTask(
                    player.getServer().getPluginManager().getPlugin("ReportSystem"),
                    () -> populateInventory(contents, reports)
                );
            });

        setupControlBar(contents, player);
    }

    private void populateInventory(InventoryContents contents, List<ReportResponse> reports) {
        var pagination = contents.pagination();
        pagination.setItemsPerPage(ITEMS_PER_PAGE);

        var items = reports.stream()
            .map(report -> {
                var material = getStatusMaterial(report.status());
                var lore = new ArrayList<String>();
                lore.add("§8ID: §7" + report.id().substring(0, 8) + "...");
                lore.add("");
                lore.add("§7Gemeldeter: §f" + report.reportedName());
                lore.add("§7Reporter: §f" + report.reporterName());
                lore.add("§7Grund: §f" + report.reason());
                lore.add("§7Server: §f" + report.server());
                lore.add("§7Datum: §f" + report.createdAt().toString().substring(0, 19));
                lore.add("");
                lore.add("§e§lLinksklick §7→ Details");
                lore.add("§a§lRechtsklick §7→ Berechtigt");
                lore.add("§c§lShift+Rechtsklick §7→ Ablehnen (Chat)");

                var prefix = report.status() == ReportStatus.OPEN ? "§c"
                    : report.status() == ReportStatus.RESOLVED ? "§a" : "§8";

                return ClickableItem.of(
                    createEnchantedItem(material,
                        prefix + "#" + report.id().substring(0, 8) + " §7- §e" + report.reportedName(),
                        lore
                    ),
                    e -> {
                        if (e.isLeftClick()) {
                            new ReportDetailGui((me.welovepaperapi.reports.api.service.ReportService) reportService, report.id()).open((Player) e.getWhoClicked());
                        } else if (e.isRightClick()) {
                            if (e.isShiftClick()) {
                                var admin = (Player) e.getWhoClicked();
                                admin.closeInventory();
                                ChatInputListener.waitForInput(admin, reason -> {
                                    reportService.rejectReport(report.id(), admin.getUniqueId(), admin.getName(), reason)
                                        .thenAccept(response ->
                                            admin.sendMessage("§a§lReport › §aReport §c#" + report.id().substring(0, 8) + "... §aabgelehnt. Grund: §f" + reason)
                                        )
                                        .exceptionally(ex -> {
                                            admin.sendMessage("§c§lReport › §c" + ex.getCause().getMessage());
                                            return null;
                                        });
                                });
                                admin.sendMessage("§e§lReport › §eGib den Ablehnungsgrund im Chat ein:");
                            } else {
                                handleResolve((Player) e.getWhoClicked(), report.id());
                            }
                        }
                    }
                );
            })
            .toArray(ClickableItem[]::new);

        pagination.setItems(items);
        var pageItems = pagination.getPageItems();
        for (int i = 0; i < pageItems.length; i++) {
            contents.set(1 + (i / INNER_COLS), 1 + (i % INNER_COLS), pageItems[i]);
        }

        var totalPages = (int) Math.ceil((double) items.length / ITEMS_PER_PAGE);
        if (totalPages == 0) totalPages = 1;

        contents.set(5, 4, ClickableItem.empty(createItem(
            Material.PAPER,
            "§7Seite §e§l" + (currentPage + 1) + "§7/§e§l" + totalPages,
            List.of("§7" + reports.size() + " Reports angezeigt")
        )));

        if (currentPage > 0) {
            contents.set(5, 1, ClickableItem.of(
                createEnchantedItem(Material.ARROW, "§e§l← Vorherige"),
                e -> {
                    currentPage--;
                    open((Player) e.getWhoClicked());
                }
            ));
        }

        if (currentPage + 1 < totalPages) {
            contents.set(5, 7, ClickableItem.of(
                createEnchantedItem(Material.ARROW, "§e§lNächste →"),
                e -> {
                    currentPage++;
                    open((Player) e.getWhoClicked());
                }
            ));
        }
    }

    private void setupControlBar(InventoryContents contents, Player player) {
        var isOpenFilter = currentFilter == ReportStatus.OPEN;

        contents.set(5, 2, ClickableItem.of(
            createEnchantedItem(
                isOpenFilter ? Material.LIME_STAINED_GLASS_PANE : Material.LIME_STAINED_GLASS,
                (isOpenFilter ? "§a✔ " : "") + "§aOffene"
            ),
            e -> {
                currentFilter = ReportStatus.OPEN;
                currentTemplateFilter = null;
                currentPage = 0;
                open(player);
            }
        ));

        contents.set(5, 5, ClickableItem.of(
            createEnchantedItem(Material.COMPASS, "§e§lStats"),
            e -> {
                player.closeInventory();
                player.performCommand("report stats");
            }
        ));

        contents.set(5, 8, ClickableItem.of(
            createEnchantedItem(Material.BARRIER, "§c§lSchließen"),
            e -> player.closeInventory()
        ));
    }

    private void handleResolve(Player player, String reportId) {
        reportService.resolveReport(reportId, player.getUniqueId(), player.getName())
            .thenAccept(response ->
                player.sendMessage("§a§lReport › §aReport §6" + reportId.substring(0, 8) + "... §aals berechtigt markiert.")
            )
            .exceptionally(ex -> {
                player.sendMessage("§c§lReport › §c" + ex.getCause().getMessage());
                return null;
            });
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
            contents.set(5, col, ClickableItem.empty(border));
        }
        for (int row = 1; row < 5; row++) {
            contents.set(row, 0, ClickableItem.empty(border));
            contents.set(row, 8, ClickableItem.empty(border));
        }
    }

    private ItemStack createEnchantedItem(Material material, String name) {
        return createEnchantedItem(material, name, List.of());
    }

    private ItemStack createEnchantedItem(Material material, String name, List<String> lore) {
        var item = new ItemStack(material);
        var meta = item.getItemMeta();
        meta.displayName(Component.text(name));
        if (!lore.isEmpty()) {
            meta.lore(lore.stream().map(Component::text).toList());
        }
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
