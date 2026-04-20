package com.dbmonitor.collector.service;

import com.dbmonitor.collector.dto.IncomingEvent;
import com.dbmonitor.collector.dto.IncomingMetrics;
import com.dbmonitor.collector.entity.*;
import com.dbmonitor.collector.repository.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Service
public class IngestService {

    private static final Logger log = LoggerFactory.getLogger(IngestService.class);

    private final ConnectionEventRepository connRepo;
    private final QueryEventRepository queryRepo;
    private final TransactionEventRepository txRepo;
    private final MetricsSnapshotRepository metricsRepo;

    public IngestService(ConnectionEventRepository connRepo,
                         QueryEventRepository queryRepo,
                         TransactionEventRepository txRepo,
                         MetricsSnapshotRepository metricsRepo) {
        this.connRepo = connRepo;
        this.queryRepo = queryRepo;
        this.txRepo = txRepo;
        this.metricsRepo = metricsRepo;
    }

    @Transactional
    public int ingestEvents(List<IncomingEvent> events) {
        if (events == null || events.isEmpty()) return 0;

        List<ConnectionEventEntity> conns = new ArrayList<>();
        List<QueryEventEntity> queries = new ArrayList<>();
        List<TransactionEventEntity> txs = new ArrayList<>();

        for (IncomingEvent e : events) {
            String op = e.getOperationType();
            if (op == null) continue;

            switch (op) {
                case "CONNECTION_OPEN":
                case "CONNECTION_CLOSE":
                    conns.add(toConnEntity(e));
                    break;
                case "QUERY":
                case "UPDATE":
                case "BATCH":
                case "OTHER":
                    queries.add(toQueryEntity(e));
                    break;
                case "COMMIT":
                case "ROLLBACK":
                    txs.add(toTxEntity(e));
                    break;
                default:
                    log.warn("Unknown operationType: {}", op);
            }
        }

        if (!conns.isEmpty())   connRepo.saveAll(conns);
        if (!queries.isEmpty()) queryRepo.saveAll(queries);
        if (!txs.isEmpty())     txRepo.saveAll(txs);

        log.debug("Ingested {} events (conn={}, query={}, tx={})",
                events.size(), conns.size(), queries.size(), txs.size());

        return events.size();
    }

    @Transactional
    public void ingestMetrics(IncomingMetrics m, String appName, String hostName) {
        MetricsSnapshotEntity e = new MetricsSnapshotEntity();
        e.setApplicationName(appName != null ? appName : "unknown-app");
        e.setHostName(hostName);
        e.setTotalQueries(m.getTotalQueries());
        e.setTotalUpdates(m.getTotalUpdates());
        e.setTotalErrors(m.getTotalErrors());
        e.setTotalLatencyNs(m.getTotalLatencyNs());
        e.setCommits(m.getCommits());
        e.setRollbacks(m.getRollbacks());
        e.setActiveConnections(m.getActiveConnections());
        e.setSlowQueries(m.getSlowQueries());
        e.setReceivedAt(System.currentTimeMillis());
        metricsRepo.save(e);
    }

    // ---- mapping helpers ----

    private ConnectionEventEntity toConnEntity(IncomingEvent e) {
        ConnectionEventEntity c = new ConnectionEventEntity();
        c.setApplicationName(e.getApplicationName());
        c.setHostName(e.getHostName());
        c.setJvmId(e.getJvmId());
        c.setDatabaseType(e.getDatabaseType());
        c.setOperationType(e.getOperationType());
        c.setTimestamp(e.getTimestamp() != null ? e.getTimestamp() : 0L);
        c.setDurationNs(e.getDurationNs() != null ? e.getDurationNs() : 0L);
        c.setSuccess(e.getSuccess() != null && e.getSuccess());
        c.setMetadata(e.getMetadata());
        return c;
    }

    private QueryEventEntity toQueryEntity(IncomingEvent e) {
        QueryEventEntity q = new QueryEventEntity();
        q.setApplicationName(e.getApplicationName());
        q.setHostName(e.getHostName());
        q.setJvmId(e.getJvmId());
        q.setDatabaseType(e.getDatabaseType());
        q.setOperationType(e.getOperationType());
        q.setTimestamp(e.getTimestamp() != null ? e.getTimestamp() : 0L);
        q.setDurationNs(e.getDurationNs() != null ? e.getDurationNs() : 0L);
        q.setSuccess(e.getSuccess() != null && e.getSuccess());
        String qText = e.getQuery();
        if (qText != null && qText.length() > 3900) qText = qText.substring(0, 3900);
        q.setQuery(qText);
        q.setSlow(e.getSlow() != null && e.getSlow());
        return q;
    }

    private TransactionEventEntity toTxEntity(IncomingEvent e) {
        TransactionEventEntity t = new TransactionEventEntity();
        t.setApplicationName(e.getApplicationName());
        t.setHostName(e.getHostName());
        t.setJvmId(e.getJvmId());
        t.setOperationType(e.getOperationType());
        t.setTimestamp(e.getTimestamp() != null ? e.getTimestamp() : 0L);
        t.setDurationNs(e.getDurationNs() != null ? e.getDurationNs() : 0L);
        t.setSuccess(e.getSuccess() != null && e.getSuccess());
        return t;
    }
}
