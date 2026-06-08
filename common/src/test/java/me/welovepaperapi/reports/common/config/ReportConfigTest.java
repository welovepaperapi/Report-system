package me.welovepaperapi.reports.common.config;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class ReportConfigTest {

    @Test
    void defaults_shouldProvideSensibleDefaults() {
        var config = ReportConfig.defaults();

        assertNotNull(config);
        assertEquals("mongodb://localhost:27017", config.mongo().uri());
        assertEquals("reports", config.mongo().database());
        assertEquals("localhost", config.redis().host());
        assertEquals(6379, config.redis().port());
        assertNull(config.redis().password());
        assertEquals("server1", config.serverName());
        assertEquals(".", config.bedrockPrefix());
        assertFalse(config.debug());
    }

    @Test
    void constructor_shouldStoreValues() {
        var config = new ReportConfig(
            new ReportConfig.MongoConfig("mongodb://custom:27017", "custom_db"),
            new ReportConfig.RedisConfig("redis.example.com", 6380, "secret"),
            "lobby-1",
            "!",
            List.of("survival-1", "survival-2"),
            true
        );

        assertEquals("mongodb://custom:27017", config.mongo().uri());
        assertEquals("custom_db", config.mongo().database());
        assertEquals("redis.example.com", config.redis().host());
        assertEquals(6380, config.redis().port());
        assertEquals("secret", config.redis().password());
        assertEquals("lobby-1", config.serverName());
        assertEquals("!", config.bedrockPrefix());
        assertEquals(2, config.allowedServers().size());
        assertTrue(config.debug());
    }
}
