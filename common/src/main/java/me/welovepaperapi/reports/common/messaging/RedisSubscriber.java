package me.welovepaperapi.reports.common.messaging;

import redis.clients.jedis.JedisPubSub;

import java.util.function.Consumer;

public class RedisSubscriber extends JedisPubSub {

    private final Consumer<String> messageHandler;

    public RedisSubscriber(Consumer<String> messageHandler) {
        this.messageHandler = messageHandler;
    }

    @Override
    public void onMessage(String channel, String message) {
        messageHandler.accept(message);
    }

    @Override
    public void onSubscribe(String channel, int subscribedChannels) {
        // subscription confirmed
    }

    @Override
    public void onUnsubscribe(String channel, int subscribedChannels) {
        // unsubscription confirmed
    }
}
