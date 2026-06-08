package me.welovepaperapi.reports.bukkit;

import com.google.inject.Guice;
import me.welovepaperapi.reports.common.config.ReportConfig;
import me.welovepaperapi.reports.common.di.CommonModule;
import me.welovepaperapi.reports.common.messaging.RedisManager;
import me.welovepaperapi.reports.api.dto.ReportNewNotification;
import me.welovepaperapi.reports.api.dto.ReportStatusUpdate;
import me.welovepaperapi.reports.api.service.ReportService;
import me.welovepaperapi.reports.bukkit.command.ReportCommand;
import me.welovepaperapi.reports.bukkit.command.ReportsCommand;
import me.welovepaperapi.reports.bukkit.gui.ChatInputListener;
import me.welovepaperapi.reports.bukkit.gui.SmartInventoryManager;
import me.welovepaperapi.reports.bukkit.listener.PlayerNotificationListener;
import me.welovepaperapi.reports.bukkit.listener.StatusUpdateListener;
import me.welovepaperapi.reports.bukkit.service.BedrockFormService;

import org.bukkit.plugin.java.JavaPlugin;

import java.util.logging.Level;

public final class ReportPlugin extends JavaPlugin {

    private com.google.inject.Injector injector;
    private RedisManager redisManager;

    @Override
    public void onEnable() {
        try {
            saveDefaultConfig();
            SmartInventoryManager.getManager();
            var config = loadConfig();

            this.injector = Guice.createInjector(new CommonModule(config));
            this.redisManager = injector.getInstance(RedisManager.class);

            var reportService = injector.getInstance(ReportService.class);
            BedrockFormService bedrockFormService = null;
            try {
                if (getServer().getPluginManager().getPlugin("floodgate") != null) {
                    bedrockFormService = new BedrockFormService(reportService, config, this);
                    getLogger().info("Floodgate found, Bedrock forms enabled");
                }
            } catch (NoClassDefFoundError e) {
                getLogger().info("Floodgate not available, Bedrock forms disabled");
            }

            ChatInputListener.init(this);
            var reportCommand = new ReportCommand(reportService, config, bedrockFormService, this);
            var reportsCommand = new ReportsCommand(reportService, this);

            var cmdReport = getCommand("report");
            if (cmdReport != null) {
                cmdReport.setExecutor(reportCommand);
                getLogger().info("Command /report registered");
            } else {
                getLogger().severe("Could not find command 'report' in plugin.yml");
            }

            var cmdReports = getCommand("reports");
            if (cmdReports != null) {
                cmdReports.setExecutor(reportsCommand);
                getLogger().info("Command /reports registered");
            } else {
                getLogger().severe("Could not find command 'reports' in plugin.yml");
            }

            setupRedisSubscribers(reportService);

            getLogger().info("ReportSystem enabled successfully");
        } catch (Exception e) {
            getLogger().log(Level.SEVERE, "Failed to enable ReportSystem", e);
            getServer().getPluginManager().disablePlugin(this);
        }
    }

    @Override
    public void onDisable() {
        if (redisManager != null) {
            redisManager.close();
        }
        getLogger().info("ReportSystem disabled");
    }

    private ReportConfig loadConfig() {
        var config = getConfig();
        return new ReportConfig(
            new ReportConfig.MongoConfig(
                config.getString("mongo.uri", "mongodb://localhost:27017"),
                config.getString("mongo.database", "reports")
            ),
            new ReportConfig.RedisConfig(
                config.getString("redis.host", "localhost"),
                config.getInt("redis.port", 6379),
                config.getString("redis.password", null)
            ),
            config.getString("server-name", "server1"),
            config.getString("bedrock-prefix", "."),
            config.getStringList("allowed-servers"),
            config.getBoolean("debug", false)
        );
    }

    private void setupRedisSubscribers(ReportService reportService) {
        var gson = new com.google.gson.Gson();

        redisManager.subscribe("reports:new", message -> {
            try {
                var notification = gson.fromJson(message, ReportNewNotification.class);
                new PlayerNotificationListener(this).onNewReport(notification);
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Failed to process report notification", e);
            }
        });

        redisManager.subscribe("reports:status_update", message -> {
            try {
                var update = gson.fromJson(message, ReportStatusUpdate.class);
                new StatusUpdateListener(this).onStatusUpdate(update);
            } catch (Exception e) {
                getLogger().log(Level.WARNING, "Failed to process status update", e);
            }
        });
    }
}
