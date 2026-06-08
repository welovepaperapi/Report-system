package me.welovepaperapi.reports.bungee;

import com.google.inject.Guice;
import me.welovepaperapi.reports.api.service.ReportService;
import me.welovepaperapi.reports.bungee.listener.LoginReminderListener;
import me.welovepaperapi.reports.common.config.ReportConfig;
import me.welovepaperapi.reports.common.di.CommonModule;
import me.welovepaperapi.reports.common.messaging.RedisManager;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.config.ConfigurationProvider;
import net.md_5.bungee.config.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.logging.Level;

public final class ReportBungeePlugin extends Plugin {

    private com.google.inject.Injector injector;
    private RedisManager redisManager;

    @Override
    public void onEnable() {
        loadConfig();
        var config = loadConfigFromFile();

        this.injector = Guice.createInjector(new CommonModule(config));
        this.redisManager = injector.getInstance(RedisManager.class);

        var reportService = injector.getInstance(ReportService.class);
        getProxy().getPluginManager().registerListener(
            this, new LoginReminderListener(reportService)
        );

        getLogger().info("ReportSystem BungeeCord enabled successfully");
    }

    @Override
    public void onDisable() {
        if (redisManager != null) {
            redisManager.close();
        }
        getLogger().info("ReportSystem BungeeCord disabled");
    }

    private void loadConfig() {
        if (!getDataFolder().exists()) {
            getDataFolder().mkdirs();
        }
        var configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            try (var in = getResourceAsStream("config.yml")) {
                if (in != null) {
                    Files.copy(in, configFile.toPath());
                }
            } catch (IOException e) {
                getLogger().log(Level.WARNING, "Failed to save default config", e);
            }
        }
    }

    private ReportConfig loadConfigFromFile() {
        var configFile = new File(getDataFolder(), "config.yml");
        if (!configFile.exists()) {
            getLogger().warning("Config file not found, using defaults");
            return ReportConfig.defaults();
        }
        try {
            var config = ConfigurationProvider.getProvider(YamlConfiguration.class).load(configFile);
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
                config.getString("server-name", "bungee-proxy"),
                config.getString("bedrock-prefix", "."),
                java.util.List.of(), // allowed-servers not used on proxy
                config.getBoolean("debug", false)
            );
        } catch (IOException e) {
            getLogger().log(Level.WARNING, "Failed to load config, using defaults", e);
            return ReportConfig.defaults();
        }
    }
}
