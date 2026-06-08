package me.welovepaperapi.reports.common.util;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import me.welovepaperapi.reports.api.enums.PlatformType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

public class UuidResolver {

    private static final Logger log = LoggerFactory.getLogger(UuidResolver.class);
    private static final String JAVA_API = "https://mc-api.io/v1/java/profile/minecraft/%s";
    private static final String BEDROCK_API = "https://mc-api.io/v1/bedrock/profile/minecraft/%s";

    private final OkHttpClient httpClient;
    private final Gson gson;
    private final Cache<String, Optional<ResolvedPlayer>> cache;
    private final String bedrockPrefix;

    public UuidResolver(String bedrockPrefix) {
        this.httpClient = new OkHttpClient.Builder()
            .connectTimeout(Duration.ofSeconds(5))
            .readTimeout(Duration.ofSeconds(10))
            .build();
        this.gson = new Gson();
        this.cache = CacheBuilder.newBuilder()
            .maximumSize(1000)
            .expireAfterWrite(Duration.ofHours(1))
            .build();
        this.bedrockPrefix = bedrockPrefix;
    }

    public CompletableFuture<Optional<ResolvedPlayer>> resolve(String playerName) {
        var cached = cache.getIfPresent(playerName);
        if (cached != null) return CompletableFuture.completedFuture(cached);

        var platform = detectPlatform(playerName);
        var apiUrl = platform == PlatformType.BEDROCK
            ? String.format(BEDROCK_API, sanitizeName(playerName, platform))
            : String.format(JAVA_API, playerName);

        return CompletableFuture.supplyAsync(() -> {
            try {
                var request = new Request.Builder()
                    .url(apiUrl)
                    .header("User-Agent", "ReportSystem/1.0")
                    .get()
                    .build();

                try (var response = httpClient.newCall(request).execute()) {
                    if (!response.isSuccessful() || response.body() == null) {
                        log.warn("API returned {} for player {}", response.code(), playerName);
                        return Optional.<ResolvedPlayer>empty();
                    }

                    var body = response.body().string();
                    var json = gson.fromJson(body, JsonObject.class);

                    if (json == null || !json.has("id")) {
                        log.warn("No UUID found for player {}", playerName);
                        return Optional.<ResolvedPlayer>empty();
                    }

                    var uuidStr = json.get("id").getAsString();
                    var uuid = parseUuid(uuidStr);
                    var name = json.has("name") ? json.get("name").getAsString() : playerName;
                    var resolved = new ResolvedPlayer(uuid, name, platform);

                    cache.put(playerName, Optional.of(resolved));
                    return Optional.of(resolved);
                }
            } catch (Exception e) {
                log.error("Failed to resolve UUID for player {}: {}", playerName, e.getMessage());
                return Optional.empty();
            }
        });
    }

    public PlatformType detectPlatform(String playerName) {
        if (playerName.startsWith(bedrockPrefix)) {
            return PlatformType.BEDROCK;
        }
        return PlatformType.JAVA;
    }

    private String sanitizeName(String playerName, PlatformType platform) {
        if (platform == PlatformType.BEDROCK && playerName.startsWith(bedrockPrefix)) {
            return playerName.substring(bedrockPrefix.length());
        }
        return playerName;
    }

    private UUID parseUuid(String uuidStr) {
        var formatted = uuidStr.contains("-") ? uuidStr
            : uuidStr.replaceFirst(
                "(\\p{XDigit}{8})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{4})(\\p{XDigit}{12})",
                "$1-$2-$3-$4-$5"
            );
        return UUID.fromString(formatted);
    }

    public record ResolvedPlayer(UUID uuid, String name, PlatformType platform) {}
}
