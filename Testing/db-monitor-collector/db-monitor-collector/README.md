# DB Monitor Collector (Backend)

Spring Boot backend for the `db-monitor-agent`. Receives events + metrics from
any JVM running the agent, stores them in H2, and exposes REST APIs for the
dashboard (frontend to be built separately).

## Stack
- Spring Boot 3.2 / Java 17
- Spring Data JPA
- H2 (file-based, persists across restarts)

## Run

```bash
cd db-monitor-collector
mvn spring-boot:run
```

Server starts on **http://localhost:8081** — the same port the agent is
already POSTing to.

Other useful URLs:
- `http://localhost:8081/h2-console` — raw DB browser
  (JDBC URL: `jdbc:h2:file:./data/dbmonitor`, user `sa`, no password)
- `http://localhost:8081/health` — liveness check

## Project layout

```
db-monitor-collector/
├── pom.xml
├── README.md
└── src/main/
    ├── resources/
    │   └── application.properties
    └── java/com/dbmonitor/collector/
        ├── CollectorApplication.java
        ├── controller/
        │   ├── IngestController.java     POST /events, /metrics
        │   └── DashboardController.java  GET /api/*
        ├── service/
        │   ├── IngestService.java        routes events by operationType
        │   └── DashboardService.java     aggregation queries
        ├── entity/                       4 JPA entities
        ├── repository/                   4 Spring Data repos
        ├── dto/                          IncomingEvent, IncomingMetrics
        └── config/WebConfig.java         CORS for API
```

## Ingest endpoints (called by agent)

| Method | Path       | Body / Headers                                              |
|--------|------------|-------------------------------------------------------------|
| POST   | `/events`  | JSON array of mixed events                                  |
| POST   | `/metrics` | `MetricsSnapshot` JSON + `X-App-Name`, `X-Host-Name` headers |

## Dashboard endpoints (called by UI later)

| Method | Path                                | Description                |
|--------|-------------------------------------|----------------------------|
| GET    | `/api/summary?minutes=5`            | Global KPIs                |
| GET    | `/api/apps?minutes=5`               | Per-app breakdown          |
| GET    | `/api/timeseries?app=X&minutes=15`  | Chart data points          |
| GET    | `/api/slow-queries?limit=20`        | Top slow queries           |
| GET    | `/api/recent-queries?limit=50`      | Most recent queries        |
| GET    | `/api/known-apps`                   | App names seen             |

## Agent-side changes needed

Two small edits to the existing agent so per-app data works properly.

### 1. `EventDispatcher.java` — tag metrics with app + make URL configurable

```java
// add near the top of the class
private static final String COLLECTOR_URL =
    System.getProperty("db.collector.url", "http://localhost:8081");

// in sendBatch(), change the URI line to:
.uri(URI.create(COLLECTOR_URL + "/events"))

// in sendMetrics(), change to:
HttpRequest request = HttpRequest.newBuilder()
        .uri(URI.create(COLLECTOR_URL + "/metrics"))
        .header("Content-Type", "application/json")
        .header("X-App-Name",  AgentConfig.APP_NAME)
        .header("X-Host-Name", HostUtil.getHostName())
        .POST(HttpRequest.BodyPublishers.ofString(json))
        .build();
```

### 2. `QueryExecutionAdvice.java` — fix the enable-flag bug

Current code skips query capture if *either* `ENABLE_QUERY` or
`ENABLE_TRANSACTION` is false. Query capture should only depend on
`ENABLE_QUERY`:

```java
// enter()
if (!AgentConfig.ENABLE_QUERY) {
    return 0L;
}

// exit()
if (!AgentConfig.ENABLE_QUERY) return;
```

After these changes, run any JVM app with:

```bash
java -javaagent:db-monitor-agent.jar \
     -Dapp.name=my-service \
     -Ddb.collector.url=http://localhost:8081 \
     -jar my-app.jar
```

## Smoke test without the agent

```bash
# post a fake query event
curl -X POST http://localhost:8081/events \
  -H "Content-Type: application/json" \
  -d '[{"applicationName":"test-app","operationType":"QUERY",
        "timestamp":1700000000000,"durationNs":5000000,
        "success":true,"query":"SELECT ?","slow":false}]'

# post a fake metrics snapshot
curl -X POST http://localhost:8081/metrics \
  -H "Content-Type: application/json" \
  -H "X-App-Name: test-app" \
  -d '{"totalQueries":10,"totalLatencyNs":50000000,
       "activeConnections":3,"slowQueries":1}'

# check it landed
curl http://localhost:8081/api/summary
```

## Still to build

- **Frontend dashboard** — will call the `/api/*` endpoints above
- **Sample test app** — a tiny Spring Boot app with JDBC calls, run under the agent to generate traffic for the demo
- **Alerting** — threshold-based log warnings from a simple `@Scheduled` job
- **Auth / TLS** between agent and collector (for production)
