package me.welovepaperapi.reports.bukkit.gui;

import me.welovepaperapi.reports.common.config.ReportConfig;
import me.welovepaperapi.reports.api.service.ReportService;
import fr.minuskube.inv.ClickableItem;
import fr.minuskube.inv.SmartInventory;
import fr.minuskube.inv.content.InventoryContents;
import fr.minuskube.inv.content.InventoryProvider;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.NamespacedKey;
import org.bukkit.Registry;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.SkullMeta;

import java.util.List;

public class ReportPlayerSelectorGui implements InventoryProvider {

    private static final int INNER_COLS = 7;
    private static final int INNER_ROWS = 4;
    private static final int ITEMS_PER_PAGE = INNER_COLS * INNER_ROWS;
    private final ReportService reportService;
    private final ReportConfig config;

    public ReportPlayerSelectorGui(ReportService reportService, ReportConfig config) {
        this.reportService = reportService;
        this.config = config;
    }

    public void open(Player player) {
        SmartInventory.builder()
            .id("reportPlayerSelector")
            .provider(this)
            .size(6, 9)
            .title("§8Spieler melden")
            .manager(SmartInventoryManager.getManager())
            .build()
            .open(player);
    }

    @Override
    public void init(Player player, InventoryContents contents) {
        fillBorder(contents);

        contents.set(0, 4, ClickableItem.empty(createItem(
            Material.LIGHT_BLUE_STAINED_GLASS_PANE,
            "§b§lSpieler melden",
            List.of("§7Wähle einen Spieler aus")
        )));

        var reporters = getTargetPlayers(player);

        var items = reporters.stream()
            .map(target -> ClickableItem.of(
                createPlayerHead(target),
                e -> new ReportTemplateGui(reportService, config, target.getUniqueId(), target.getName()).open(player)
            ))
            .toArray(ClickableItem[]::new);

        var pagination = contents.pagination();
        pagination.setItems(items);
        pagination.setItemsPerPage(ITEMS_PER_PAGE);

        populateInnerGrid(contents, pagination);
        setupNavigation(contents, player, pagination, items.length);
    }

    private List<Player> getTargetPlayers(Player reporter) {
        return Bukkit.getOnlinePlayers().stream()
            .filter(p -> !p.getUniqueId().equals(reporter.getUniqueId()))
            .toList();
    }

    private void populateInnerGrid(InventoryContents contents, fr.minuskube.inv.content.Pagination pagination) {
        var pageItems = pagination.getPageItems();
        for (int i = 0; i < pageItems.length; i++) {
            contents.set(1 + (i / INNER_COLS), 1 + (i % INNER_COLS), pageItems[i]);
        }
    }

    private void setupNavigation(InventoryContents contents, Player player,
                                  fr.minuskube.inv.content.Pagination pagination, int totalItems) {
        var totalPages = (int) Math.ceil((double) totalItems / ITEMS_PER_PAGE);
        var currentPage = pagination.getPage() + 1;

        if (!pagination.isFirst()) {
            contents.set(5, 1, ClickableItem.of(
                createEnchantedItem(Material.ARROW, "§e§l← Vorherige"),
                e -> openPage(player, pagination.getPage() - 1)
            ));
        }

        contents.set(5, 4, ClickableItem.empty(createItem(
            Material.PAPER,
            "§7Seite §e§l" + currentPage + "§7/§e§l" + Math.max(totalPages, 1),
            List.of("§7Spieler: §f" + totalItems)
        )));

        if (!pagination.isLast()) {
            contents.set(5, 7, ClickableItem.of(
                createEnchantedItem(Material.ARROW, "§e§lNächste →"),
                e -> openPage(player, pagination.getPage() + 1)
            ));
        }

        contents.set(5, 8, ClickableItem.of(
            createEnchantedItem(Material.BARRIER, "§c§lSchließen"),
            e -> player.closeInventory()
        ));
    }

    private void openPage(Player player, int page) {
        SmartInventory.builder()
            .id("reportPlayerSelector")
            .provider(new ReportPlayerSelectorGui(reportService, config))
            .size(6, 9)
            .title("§8Spieler melden")
            .manager(SmartInventoryManager.getManager())
            .build()
            .open(player, page);
    }

    @Override
    public void update(Player player, InventoryContents contents) {}

    private ItemStack createPlayerHead(Player target) {
        var item = new ItemStack(Material.PLAYER_HEAD);
        var meta = (SkullMeta) item.getItemMeta();
        meta.displayName(Component.text("§e§l" + target.getName()));
        meta.lore(List.of(
            Component.text("§7Klicken, um §e" + target.getName() + " §7zu melden"),
            Component.text(""),
            Component.text("§a§lKlick §7→ Grund auswählen")
        ));
        meta.setOwningPlayer(target);
        meta.addItemFlags(ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
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
