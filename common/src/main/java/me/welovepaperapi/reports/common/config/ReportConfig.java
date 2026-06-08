package me.welovepaperapi.reports.common.config;

import org.jetbrains.annotations.Nullable;

import java.util.List;

public record ReportConfig(
    MongoConfig mongo,
    RedisConfig redis,
    String serverName,
    String bedrockPrefix,
    List<String> allowedServers,
    boolean debug
) {
    public static ReportConfig defaults() {
        return new ReportConfig(
            new MongoConfig("mongodb://localhost:27017", "reports"),
            new RedisConfig("localhost", 6379, null),
            "server1",
            ".",
            List.of("*"),
            false
        );
    }

    public record MongoConfig(
        String uri,
        String database
    ) {}

    public record RedisConfig(
        String host,
        int port,
        @Nullable String password
    ) {}
}
