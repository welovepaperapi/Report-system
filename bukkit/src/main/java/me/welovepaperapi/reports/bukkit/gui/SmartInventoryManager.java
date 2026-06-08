package me.welovepaperapi.reports.bukkit.gui;

import fr.minuskube.inv.InventoryManager;
import org.bukkit.Bukkit;
import org.bukkit.plugin.java.JavaPlugin;

public final class SmartInventoryManager {

    private static InventoryManager manager;

    private SmartInventoryManager() {}

    public static InventoryManager getManager() {
        if (manager == null) {
            var plugin = (JavaPlugin) Bukkit.getPluginManager().getPlugin("ReportSystem");
            if (plugin == null) {
                throw new IllegalStateException("ReportSystem plugin not found");
            }
            manager = new InventoryManager(plugin);
            manager.init();
        }
        return manager;
    }
}
