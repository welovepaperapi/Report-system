package me.welovepaperapi.reports.common.database;

import com.mongodb.ConnectionString;
import com.mongodb.MongoClientSettings;
import com.mongodb.MongoException;
import com.mongodb.client.MongoClient;
import com.mongodb.client.MongoClients;
import me.welovepaperapi.reports.common.config.ReportConfig;
import dev.morphia.Datastore;
import dev.morphia.Morphia;
import org.bson.UuidRepresentation;
import org.bson.codecs.configuration.CodecRegistries;
import org.bson.codecs.pojo.PojoCodecProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

public class MongoManager implements AutoCloseable {

    private static final Logger log = LoggerFactory.getLogger(MongoManager.class);

    private final MongoClient client;
    private final Datastore datastore;
    private final ReportRepository reportRepository;

    public MongoManager(ReportConfig.MongoConfig config) {
        var codecRegistry = CodecRegistries.fromRegistries(
            MongoClientSettings.getDefaultCodecRegistry(),
            CodecRegistries.fromProviders(PojoCodecProvider.builder().automatic(true).build())
        );

        var settings = MongoClientSettings.builder()
            .applyConnectionString(new ConnectionString(config.uri()))
            .uuidRepresentation(UuidRepresentation.STANDARD)
            .codecRegistry(codecRegistry)
            .applyToSocketSettings(builder ->
                builder.connectTimeout(5, TimeUnit.SECONDS)
                    .readTimeout(10, TimeUnit.SECONDS))
            .applyToClusterSettings(builder ->
                builder.serverSelectionTimeout(5, TimeUnit.SECONDS))
            .build();

        this.client = MongoClients.create(settings);
        this.datastore = Morphia.createDatastore(client, config.database());
        this.datastore.getMapper().map(ReportModel.class);
        this.datastore.ensureIndexes();
        this.reportRepository = new ReportRepository(this.datastore);

        log.info("MongoDB connection established to database: {}", config.database());
    }

    public Datastore getDatastore() {
        return datastore;
    }

    public ReportRepository getReportRepository() {
        return reportRepository;
    }

    public boolean isConnected() {
        try {
            datastore.getDatabase().runCommand(new org.bson.Document("ping", 1));
            return true;
        } catch (MongoException e) {
            return false;
        }
    }

    @Override
    public void close() {
        if (client != null) {
            client.close();
            log.info("MongoDB connection closed");
        }
    }
}
