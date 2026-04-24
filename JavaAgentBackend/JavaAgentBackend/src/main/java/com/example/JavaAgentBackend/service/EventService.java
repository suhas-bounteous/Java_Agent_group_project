package com.example.JavaAgentBackend.service;

import com.example.JavaAgentBackend.dto.EventDTO;
import com.example.JavaAgentBackend.dto.EventStatsDTO;
import com.example.JavaAgentBackend.entity.DbConnectionEventEntity;
import com.example.JavaAgentBackend.entity.DbQueryEventEntity;
import com.example.JavaAgentBackend.entity.DbTransactionEventEntity;
import com.example.JavaAgentBackend.repository.DbConnectionEventRepository;
import com.example.JavaAgentBackend.repository.DbQueryEventRepository;
import com.example.JavaAgentBackend.repository.DbTransactionEventRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class EventService {

    @Autowired private DbConnectionEventRepository connectionRepo;
    @Autowired private DbQueryEventRepository queryRepo;
    @Autowired private DbTransactionEventRepository transactionRepo;

    // ── Called by agent POST ──────────────────────────────────────────────────

    public void processEvents(List<EventDTO> events) {
        List<DbConnectionEventEntity>  connections  = new ArrayList<>();
        List<DbQueryEventEntity>       queries      = new ArrayList<>();
        List<DbTransactionEventEntity> transactions = new ArrayList<>();

        for (EventDTO e : events) {
            String op = e.getOperationType();
            if (op == null) continue;

            switch (op) {
                case "CONNECTION_OPEN":
                case "CONNECTION_CLOSE":
                    connections.add(toConnectionEntity(e));
                    break;
                case "QUERY":
                case "UPDATE":
                case "BATCH":
                    queries.add(toQueryEntity(e));
                    break;
                case "COMMIT":
                case "ROLLBACK":
                    transactions.add(toTransactionEntity(e));
                    break;
                default:
                    System.out.println("Unknown event type: " + op);
            }
        }

        if (!connections.isEmpty())  connectionRepo.saveAll(connections);
        if (!queries.isEmpty())      queryRepo.saveAll(queries);
        if (!transactions.isEmpty()) transactionRepo.saveAll(transactions);
    }

    // ── Called by dashboard GET endpoints — read only, no writes ─────────────

    public List<EventDTO> getRecentEvents(int limit, String appName, String operationType) {
        List<EventDTO> result = new ArrayList<>();

        // Fetch from each table
        queryRepo.findAllByOrderByTimestampDesc(PageRequest.of(0, limit))
                .forEach(e -> result.add(fromQueryEntity(e)));

        connectionRepo.findAllByOrderByTimestampDesc(PageRequest.of(0, limit / 3 + 1))
                .forEach(e -> result.add(fromConnectionEntity(e)));

        transactionRepo.findAllByOrderByTimestampDesc(PageRequest.of(0, limit / 3 + 1))
                .forEach(e -> result.add(fromTransactionEntity(e)));

        // Filter
        return result.stream()
                .filter(e -> appName == null || appName.isEmpty()
                        || appName.equals(e.getApplicationName()))
                .filter(e -> operationType == null || operationType.isEmpty()
                        || operationType.equals(e.getOperationType()))
                .sorted(Comparator.comparingLong(EventDTO::getTimestamp).reversed())
                .limit(limit)
                .collect(Collectors.toList());
    }

    public List<EventDTO> getSlowQueries(int limit) {
        return queryRepo.findBySlowTrueOrderByDurationNsDesc(PageRequest.of(0, limit))
                .stream().map(this::fromQueryEntity).collect(Collectors.toList());
    }

    public List<String> getApplicationNames() {
        Set<String> names = new LinkedHashSet<>();
        names.addAll(queryRepo.findDistinctApplicationNames());
        names.addAll(connectionRepo.findDistinctApplicationNames());
        return new ArrayList<>(names);
    }

    public EventStatsDTO getStats() {
        EventStatsDTO stats = new EventStatsDTO();

        long qc = queryRepo.count();
        long cc = connectionRepo.count();
        long tc = transactionRepo.count();

        stats.setQueryCount(qc);
        stats.setConnectionCount(cc);
        stats.setTransactionCount(tc);
        stats.setTotalEvents(qc + cc + tc);
        stats.setErrorCount(queryRepo.countBySuccessFalse());

        // By application
        Map<String, Long> byApp = new LinkedHashMap<>();
        for (String app : queryRepo.findDistinctApplicationNames()) {
            byApp.put(app, queryRepo.countByApplicationName(app));
        }
        stats.setByApplication(byApp);

        // By operation type
        Map<String, Long> byOp = new LinkedHashMap<>();
        byOp.put("QUERY",            queryRepo.countByOperationType("QUERY"));
        byOp.put("UPDATE",           queryRepo.countByOperationType("UPDATE"));
        byOp.put("BATCH",            queryRepo.countByOperationType("BATCH"));
        byOp.put("COMMIT",           transactionRepo.countByOperationType("COMMIT"));
        byOp.put("ROLLBACK",         transactionRepo.countByOperationType("ROLLBACK"));
        byOp.put("CONNECTION_OPEN",  connectionRepo.countByOperationType("CONNECTION_OPEN"));
        byOp.put("CONNECTION_CLOSE", connectionRepo.countByOperationType("CONNECTION_CLOSE"));
        stats.setByOperationType(byOp);

        return stats;
    }

    // ── Mapping: entity → DTO ────────────────────────────────────────────────

    private EventDTO fromQueryEntity(DbQueryEventEntity e) {
        EventDTO dto = new EventDTO();
        dto.setApplicationName(e.getApplicationName());
        dto.setHostName(e.getHostName());
        dto.setJvmId(e.getJvmId());
        dto.setDatabaseType(e.getDatabaseType());
        dto.setOperationType(e.getOperationType());
        dto.setTimestamp(e.getTimestamp());
        dto.setDurationNs(e.getDurationNs());
        dto.setSuccess(e.isSuccess());
        dto.setQuery(e.getQuery());
        dto.setSlow(e.isSlow());
        return dto;
    }

    private EventDTO fromConnectionEntity(DbConnectionEventEntity e) {
        EventDTO dto = new EventDTO();
        dto.setApplicationName(e.getApplicationName());
        dto.setHostName(e.getHostName());
        dto.setJvmId(e.getJvmId());
        dto.setDatabaseType(e.getDatabaseType());
        dto.setOperationType(e.getOperationType());
        dto.setTimestamp(e.getTimestamp());
        dto.setDurationNs(e.getDurationNs());
        dto.setSuccess(e.isSuccess());
        dto.setMetadata(e.getMetadata());
        return dto;
    }

    private EventDTO fromTransactionEntity(DbTransactionEventEntity e) {
        EventDTO dto = new EventDTO();
        dto.setApplicationName(e.getApplicationName());
        dto.setHostName(e.getHostName());
        dto.setJvmId(e.getJvmId());
        dto.setOperationType(e.getOperationType());
        dto.setTimestamp(e.getTimestamp());
        dto.setDurationNs(e.getDurationNs());
        dto.setSuccess(e.isSuccess());
        return dto;
    }

    // ── Mapping: DTO → entity ────────────────────────────────────────────────

    private DbConnectionEventEntity toConnectionEntity(EventDTO e) {
        DbConnectionEventEntity en = new DbConnectionEventEntity();
        en.setApplicationName(e.getApplicationName());
        en.setHostName(e.getHostName());
        en.setJvmId(e.getJvmId());
        en.setDatabaseType(e.getDatabaseType());
        en.setOperationType(e.getOperationType());
        en.setTimestamp(e.getTimestamp());
        en.setDurationNs(e.getDurationNs());
        en.setSuccess(e.isSuccess());
        en.setMetadata(e.getMetadata());
        return en;
    }

    private DbQueryEventEntity toQueryEntity(EventDTO e) {
        DbQueryEventEntity en = new DbQueryEventEntity();
        en.setApplicationName(e.getApplicationName());
        en.setHostName(e.getHostName());
        en.setJvmId(e.getJvmId());
        en.setDatabaseType(e.getDatabaseType());
        en.setOperationType(e.getOperationType());
        en.setTimestamp(e.getTimestamp());
        en.setDurationNs(e.getDurationNs());
        en.setSuccess(e.isSuccess());
        en.setQuery(e.getQuery());
        en.setSlow(e.getSlow() != null && e.getSlow());
        return en;
    }

    private DbTransactionEventEntity toTransactionEntity(EventDTO e) {
        DbTransactionEventEntity en = new DbTransactionEventEntity();
        en.setApplicationName(e.getApplicationName());
        en.setHostName(e.getHostName());
        en.setJvmId(e.getJvmId());
        en.setOperationType(e.getOperationType());
        en.setTimestamp(e.getTimestamp());
        en.setDurationNs(e.getDurationNs());
        en.setSuccess(e.isSuccess());
        return en;
    }
}
