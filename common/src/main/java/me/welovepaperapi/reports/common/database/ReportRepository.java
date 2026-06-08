package me.welovepaperapi.reports.common.database;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import me.welovepaperapi.reports.api.dto.ReportResponse;
import me.welovepaperapi.reports.api.dto.StatsResponse;
import me.welovepaperapi.reports.api.enums.ReportStatus;
import me.welovepaperapi.reports.api.enums.ReportTemplate;
import me.welovepaperapi.reports.common.util.ReportMapper;
import dev.morphia.Datastore;
import dev.morphia.query.FindOptions;
import dev.morphia.query.Sort;
import dev.morphia.query.filters.Filter;
import dev.morphia.query.filters.Filters;
import org.bson.types.ObjectId;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class ReportRepository {

    private final Datastore datastore;
    private final Cache<String, Optional<ReportResponse>> reportCache;

    public ReportRepository(Datastore datastore) {
        this.datastore = datastore;
        this.reportCache = CacheBuilder.newBuilder()
            .maximumSize(500)
            .expireAfterWrite(5, TimeUnit.MINUTES)
            .build();
    }

    public CompletableFuture<ReportResponse> insert(ReportModel report) {
        return CompletableFuture.supplyAsync(() -> {
            datastore.save(report);
            var response = ReportMapper.toResponse(report);
            reportCache.put(response.id(), Optional.of(response));
            return response;
        });
    }

    public CompletableFuture<Optional<ReportResponse>> findById(String id) {
        var cached = reportCache.getIfPresent(id);
        if (cached != null) return CompletableFuture.completedFuture(cached);

        return CompletableFuture.supplyAsync(() -> {
            var entity = datastore.find(ReportModel.class)
                .filter(Filters.eq("_id", new ObjectId(id)))
                .first();
            if (entity == null) return Optional.<ReportResponse>empty();
            var response = ReportMapper.toResponse(entity);
            reportCache.put(id, Optional.of(response));
            return Optional.of(response);
        });
    }

    public CompletableFuture<List<ReportResponse>> findReports(
        @Nullable ReportStatus status,
        @Nullable ReportTemplate template,
        @Nullable String server,
        @Nullable Instant since,
        int page,
        int size
    ) {
        return CompletableFuture.supplyAsync(() -> {
            var query = datastore.find(ReportModel.class);

            var filters = new ArrayList<Filter>();
            if (status != null) filters.add(Filters.eq("status", status));
            if (template != null) filters.add(Filters.eq("template", template.name()));
            if (server != null) filters.add(Filters.eq("server", server));
            if (since != null) filters.add(Filters.gte("createdAt", since));

            if (!filters.isEmpty()) {
                query.filter(filters.toArray(new Filter[0]));
            }

            var options = new FindOptions()
                .sort(Sort.descending("createdAt"))
                .skip(page * size)
                .limit(size);

            return query
                .stream(options)
                .map(ReportMapper::toResponse)
                .toList();
        });
    }

    public CompletableFuture<Void> updateStatus(
        String id, ReportStatus status, UUID handledBy, String handledByName, @Nullable String modNote
    ) {
        return CompletableFuture.supplyAsync(() -> {
            var entity = datastore.find(ReportModel.class)
                .filter(Filters.eq("_id", new ObjectId(id)))
                .first();
            if (entity == null) {
                throw new IllegalArgumentException("Report not found: " + id);
            }
            entity.setStatus(status);
            entity.setHandledBy(handledBy);
            entity.setHandledByName(handledByName);
            entity.setHandledAt(Instant.now());
            if (modNote != null) entity.setModNote(modNote);
            datastore.merge(entity);
            reportCache.invalidate(id);
            return null;
        });
    }

    public CompletableFuture<StatsResponse> getStats() {
        return CompletableFuture.supplyAsync(() -> {
            long total = datastore.find(ReportModel.class).count();
            long open = datastore.find(ReportModel.class)
                .filter(Filters.eq("status", ReportStatus.OPEN))
                .count();
            long resolved = datastore.find(ReportModel.class)
                .filter(Filters.eq("status", ReportStatus.RESOLVED))
                .count();
            long rejected = datastore.find(ReportModel.class)
                .filter(Filters.eq("status", ReportStatus.REJECTED))
                .count();
            return new StatsResponse(total, open, resolved, rejected);
        });
    }

    public CompletableFuture<StatsResponse> getPlayerStats(UUID player) {
        return CompletableFuture.supplyAsync(() -> {
            var filter = Filters.eq("reported", player);
            long total = datastore.find(ReportModel.class).filter(filter).count();
            long open = datastore.find(ReportModel.class)
                .filter(filter, Filters.eq("status", ReportStatus.OPEN))
                .count();
            long resolved = datastore.find(ReportModel.class)
                .filter(filter, Filters.eq("status", ReportStatus.RESOLVED))
                .count();
            long rejected = datastore.find(ReportModel.class)
                .filter(filter, Filters.eq("status", ReportStatus.REJECTED))
                .count();
            return new StatsResponse(total, open, resolved, rejected);
        });
    }

    public CompletableFuture<Long> getOpenReportCount() {
        return CompletableFuture.supplyAsync(() ->
            datastore.find(ReportModel.class)
                .filter(Filters.eq("status", ReportStatus.OPEN))
                .count()
        );
    }
}
