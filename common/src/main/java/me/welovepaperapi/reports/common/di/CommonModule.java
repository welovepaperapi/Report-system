package me.welovepaperapi.reports.common.di;

import com.google.inject.AbstractModule;
import com.google.inject.Provides;
import com.google.inject.Singleton;
import me.welovepaperapi.reports.api.service.ReportService;
import me.welovepaperapi.reports.common.config.ReportConfig;
import me.welovepaperapi.reports.common.database.MongoManager;
import me.welovepaperapi.reports.common.database.ReportRepository;
import me.welovepaperapi.reports.common.messaging.RedisManager;
import me.welovepaperapi.reports.common.messaging.RedisPublisher;
import me.welovepaperapi.reports.common.service.ReportServiceImpl;
import me.welovepaperapi.reports.common.util.UuidResolver;

public class CommonModule extends AbstractModule {

    private final ReportConfig config;

    public CommonModule(ReportConfig config) {
        this.config = config;
    }

    @Override
    protected void configure() {
        bind(ReportConfig.class).toInstance(config);
    }

    @Provides
    @Singleton
    public MongoManager provideMongoManager() {
        return new MongoManager(config.mongo());
    }

    @Provides
    @Singleton
    public ReportRepository provideReportRepository(MongoManager mongoManager) {
        return mongoManager.getReportRepository();
    }

    @Provides
    @Singleton
    public RedisManager provideRedisManager() {
        return new RedisManager(config.redis());
    }

    @Provides
    @Singleton
    public RedisPublisher provideRedisPublisher(RedisManager redisManager) {
        return redisManager.getPublisher();
    }

    @Provides
    @Singleton
    public UuidResolver provideUuidResolver() {
        return new UuidResolver(config.bedrockPrefix());
    }

    @Provides
    @Singleton
    public ReportService provideReportService(
        ReportRepository repository, RedisPublisher publisher
    ) {
        return new ReportServiceImpl(repository, publisher, config.serverName());
    }
}
