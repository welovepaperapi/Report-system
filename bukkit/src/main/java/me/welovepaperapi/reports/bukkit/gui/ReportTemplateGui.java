package me.welovepaperapi.reports.bukkit.gui;

import me.welovepaperapi.reports.common.config.ReportConfig;
import me.welovepaperapi.reports.api.dto.ReportCreateRequest;
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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class ReportTemplateGui implements InventoryProvider {

    private static final int[] SLOTS = { 11, 12, 13, 14, 15, 22 };

    private final ReportService reportService;
    private final ReportConfig config;
    private final UUID targetUuid;
    private final String targetName;

    private static final Map<ReportTemplate, Material> MATERIALS = new HashMap<>();

    static {
        MATERIALS.put(ReportTemplate.CHEATING, Material.DIAMOND_SWORD);
        MATERIALS.put(ReportTemplate.INSULT, Material.PAPER);
        MATERIALS.put(ReportTemplate.BUGUSING, Material.REPEATER);
        MATERIALS.put(ReportTemplate.GRIEFING, Material.TNT);
        MATERIALS.put(ReportTemplate.SPAM, Material.BOOK);
        MATERIALS.put(ReportTemplate.OTHER, Material.COMPASS);
    }

    public ReportTemplateGui(ReportService reportService, ReportConfig config, UUID targetUuid, String targetName) {
        this.reportService = reportService;
        this.config = config;
        this.targetUuid = targetUuid;
        this.targetName = targetName;
    }

    public void open(Player player) {
        SmartInventory.builder()
            .id("reportTemplate")
            .provider(this)
            .size(4, 9)
            .title("§8Grund auswählen")
            .manager(SmartInventoryManager.getManager())
            .build()
            .open(player);
    }

    @Override
    public void init(Player player, InventoryContents contents) {
        fillBorder(contents);

        contents.set(0, 4, ClickableItem.empty(createItem(
            Material.PLAYER_HEAD,
            "§e§l" + targetName,
            List.of("§7Wähle einen Grund für deinen Report")
        )));

        int i = 0;
        for (var template : ReportTemplate.values()) {
            var material = MATERIALS.getOrDefault(template, Material.PAPER);
            var lore = new java.util.ArrayList<String>();
            lore.add("§7" + template.getDescription());
            lore.add("");
            if (template == ReportTemplate.OTHER) {
                lore.add("§e→ Grund im Chat eingeben");
                lore.add("§7(Schreibe 'abbrechen' zum Abbrechen)");
            } else {
                lore.add("§a§lKlick §7→ Report senden");
            }

            var slot = SLOTS[i];
            contents.set(slot / 9, slot % 9, ClickableItem.of(
                createEnchantedItem(material, "§6§l" + template.getDisplayName(), lore),
                e -> handleTemplateSelect(player, template)
            ));
            i++;
        }

        contents.set(3, 8, ClickableItem.of(
            createEnchantedItem(Material.BARRIER, "§c§lAbbrechen"),
            e -> player.closeInventory()
        ));
    }

    @Override
    public void update(Player player, InventoryContents contents) {}

    private void handleTemplateSelect(Player player, ReportTemplate template) {
        if (template == ReportTemplate.OTHER) {
            player.closeInventory();
            player.sendMessage("§e§lReport › §7Bitte gib deinen Grund im Chat ein:");
            player.sendMessage("§7(Schreibe 'abbrechen' um abzubrechen)");
            ChatInputListener.waitForInput(player, input -> {
                if (input.equalsIgnoreCase("abbrechen")) {
                    player.sendMessage("§cReport abgebrochen.");
                    return;
                }
                submitReport(player, template, input);
            });
            return;
        }
        submitReport(player, template, null);
    }

    private void submitReport(Player player, ReportTemplate template, String customReason) {
        var request = new ReportCreateRequest(
            player.getUniqueId(), targetUuid,
            player.getName(), targetName,
            template, customReason,
            config.serverName()
        );
        reportService.createReport(request).thenAccept(response -> {
            player.sendMessage("§a§lReport › §aDein Report gegen §6" + targetName + " §awurde aufgenommen.");
            player.sendMessage("§7  Grund: §f" + response.reason());
            player.sendMessage("§7  ID: §f" + response.id());
        }).exceptionally(ex -> {
            player.sendMessage("§c§lReport › §cFehler: " + ex.getMessage());
            return null;
        });
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

    private ItemStack createEnchantedItem(Material material, String name, List<String> lore) {
        var item = new ItemStack(material);
        var meta = item.getItemMeta();
        meta.displayName(Component.text(name));
        meta.lore(lore.stream().map(Component::text).toList());
        var ench = Registry.ENCHANTMENT.get(NamespacedKey.minecraft("unbreaking"));
        if (ench != null) meta.addEnchant(ench, 1, true);
        meta.addItemFlags(ItemFlag.HIDE_ENCHANTS, ItemFlag.HIDE_ATTRIBUTES);
        item.setItemMeta(meta);
        return item;
    }

    private ItemStack createEnchantedItem(Material material, String name) {
        return createEnchantedItem(material, name, List.of());
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
