package me.welovepaperapi.reports.common.messaging;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonPrimitive;
import com.google.gson.JsonSerializer;
import com.google.gson.JsonDeserializer;
import me.welovepaperapi.reports.api.dto.ReportNewNotification;
import me.welovepaperapi.reports.api.dto.ReportStatusUpdate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Instant;

public class RedisPublisher {

    private static final Logger log = LoggerFactory.getLogger(RedisPublisher.class);
    private static final String CHANNEL_NEW = "reports:new";
    private static final String CHANNEL_STATUS = "reports:status_update";

    private final RedisManager redisManager;
    private final Gson gson;

    public RedisPublisher(RedisManager redisManager) {
        this.redisManager = redisManager;
        this.gson = new GsonBuilder()
            .registerTypeAdapter(Instant.class, (JsonSerializer<Instant>) (src, typeOfSrc, context) ->
                new JsonPrimitive(src.toString()))
            .registerTypeAdapter(Instant.class, (JsonDeserializer<Instant>) (json, typeOfT, context) ->
                Instant.parse(json.getAsString()))
            .create();
    }

    public void publishNewReport(ReportNewNotification notification) {
        publish(CHANNEL_NEW, notification);
    }

    public void publishStatusUpdate(ReportStatusUpdate update) {
        publish(CHANNEL_STATUS, update);
    }

    private void publish(String channel, Object data) {
        try (var jedis = redisManager.getResource()) {
            var message = gson.toJson(data);
            jedis.publish(channel, message);
            log.debug("Published message to {}: {}", channel, message);
        } catch (Exception e) {
            log.error("Failed to publish message to {}: {}", channel, e.getMessage());
        }
    }
}
