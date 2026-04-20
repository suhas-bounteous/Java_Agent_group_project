# DB Test App

Tiny JDBC loop that generates traffic for testing the agent + collector pipeline.
Uses an in-memory H2 database, so no external setup is needed.

Exercises every operation the agent intercepts:
- `getConnection` / `close`
- `prepareStatement`
- `executeUpdate` (INSERT, UPDATE)
- `executeQuery` (SELECT)
- `executeBatch`
- `commit` / `rollback`

Every 5th iteration runs a deliberately slow cross-join query so you can see
the slow-query path too.

## Build

```powershell
mvn clean package
```

Produces `target\test-app.jar`.

## Run with the agent attached

```powershell
java `
  "-javaagent:C:\path\to\db-monitor-agent.jar" `
  "-Dapp.name=test-app" `
  "-Ddb.collector.url=http://localhost:8081" `
  -jar target\test-app.jar
```

(The backtick ` is the PowerShell line-continuation character.)

Replace `C:\path\to\db-monitor-agent.jar` with the actual agent jar path.

You should see output like:
```
DbTestApp started — generating traffic. Ctrl+C to stop.
[0] committed
[1] committed
[2] committed
...
[7] rolled back
```

## Verify data is flowing into the collector

In another PowerShell window:

```powershell
Invoke-RestMethod http://localhost:8081/api/summary
Invoke-RestMethod http://localhost:8081/api/recent-queries
Invoke-RestMethod http://localhost:8081/api/apps
```

`totalCalls` should climb over time and `test-app` should appear under apps.
