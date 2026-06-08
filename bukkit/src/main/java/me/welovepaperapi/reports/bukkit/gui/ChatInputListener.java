package me.welovepaperapi.reports.bukkit.gui;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.plugin.Plugin;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;

public class ChatInputListener implements Listener {

    private static final Map<UUID, Consumer<String>> pendingInputs = new ConcurrentHashMap<>();
    private static ChatInputListener instance;
    private static Plugin pluginRef;

    private ChatInputListener() {}

    public static void init(Plugin plugin) {
        pluginRef = plugin;
    }

    public static void waitForInput(Player player, Consumer<String> callback) {
        if (pluginRef == null) {
            player.sendMessage("§cSystem nicht bereit. Bitte versuche es später erneut.");
            return;
        }
        if (instance == null) {
            instance = new ChatInputListener();
            Bukkit.getPluginManager().registerEvents(instance, pluginRef);
        }
        pendingInputs.put(player.getUniqueId(), callback);
        player.closeInventory();
    }

    @EventHandler
    public void onPlayerChat(AsyncPlayerChatEvent event) {
        var callback = pendingInputs.remove(event.getPlayer().getUniqueId());
        if (callback == null) return;

        event.setCancelled(true);
        callback.accept(event.getMessage());
    }

    public static void cleanup() {
        pendingInputs.clear();
        if (instance != null) {
            HandlerList.unregisterAll(instance);
            instance = null;
        }
    }
}
