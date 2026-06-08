package me.welovepaperapi.reports.common.messaging;

import me.welovepaperapi.reports.common.config.ReportConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;
import redis.clients.jedis.JedisPoolConfig;
import redis.clients.jedis.exceptions.JedisException;

import java.time.Duration;
import java.util.function.Consumer;

public class RedisManager implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(RedisManager.class);

    private final JedisPool pool;
    private final RedisPublisher publisher;
    private RedisSubscriber subscriber;
    private Thread subscriberThread;

    public RedisManager(ReportConfig.RedisConfig config) {
        var poolConfig = new JedisPoolConfig();
        poolConfig.setMaxTotal(16);
        poolConfig.setMaxIdle(8);
        poolConfig.setMinIdle(2);
        poolConfig.setTestOnBorrow(true);
        poolConfig.setTestWhileIdle(true);
        poolConfig.setMinEvictableIdleDuration(Duration.ofSeconds(30));
        poolConfig.setTimeBetweenEvictionRuns(Duration.ofSeconds(10));

        if (config.password() != null && !config.password().isBlank()) {
            this.pool = new JedisPool(poolConfig, config.host(), config.port(), 5000, config.password());
        } else {
            this.pool = new JedisPool(poolConfig, config.host(), config.port(), 5000);
        }

        this.publisher = new RedisPublisher(this);

        log.info("Redis connection established to {}:{}", config.host(), config.port());
    }

    public Jedis getResource() {
        return pool.getResource();
    }

    public RedisPublisher getPublisher() {
        return publisher;
    }

    public void subscribe(String channel, Consumer<String> messageHandler) {
        if (subscriber != null) {
            log.warn("Subscriber already running for channel: {}", channel);
            return;
        }

        this.subscriber = new RedisSubscriber(messageHandler);
        this.subscriberThread = new Thread(() -> {
            while (!Thread.currentThread().isInterrupted()) {
                try (var jedis = pool.getResource()) {
                    jedis.subscribe(subscriber, channel);
                } catch (JedisException e) {
                    if (!Thread.currentThread().isInterrupted()) {
                        log.error("Redis subscription error, reconnecting in 5s", e);
                        try {
                            Thread.sleep(5000);
                        } catch (InterruptedException ie) {
                            Thread.currentThread().interrupt();
                            break;
                        }
                    }
                }
            }
        }, "redis-subscriber");
        subscriberThread.setDaemon(true);
        subscriberThread.start();

        log.info("Subscribed to Redis channel: {}", channel);
    }

    public void unsubscribe() {
        if (subscriber != null) {
            subscriber.unsubscribe();
            subscriber = null;
        }
        if (subscriberThread != null) {
            subscriberThread.interrupt();
            subscriberThread = null;
        }
    }

    public boolean isConnected() {
        try (var jedis = pool.getResource()) {
            jedis.ping();
            return true;
        } catch (JedisException e) {
            return false;
        }
    }

    @Override
    public void close() {
        unsubscribe();
        if (pool != null && !pool.isClosed()) {
            pool.close();
            log.info("Redis connection closed");
        }
    }
}
