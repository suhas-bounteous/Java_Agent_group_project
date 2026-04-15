package com.dbmonitor.agent.sender;

import com.dbmonitor.agent.config.AgentConfig;
import com.dbmonitor.agent.logging.AgentLogger;
import com.dbmonitor.agent.model.MetricPayload;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

/**
 * {@link MetricSender} that POSTs JSON-encoded {@link MetricPayload} batches to
 * the configured HTTP collector endpoint using OkHttp.
 *
 * <h3>Error handling contract</h3>
 * <ul>
 *   <li>HTTP 4xx / 5xx — logs a warning, does not throw, does not retry
 *       (the server made a deliberate decision).</li>
 *   <li>{@link IOException} — logs a warning; the {@link RetryPolicy} retries
 *       up to {@code config.getRetryAttempts()} times before giving up.</li>
 *   <li>Any other {@link Exception} — logs an error and swallows it so the
 *       caller (the queue drain task) is never crashed.</li>
 * </ul>
 *
 * <p>The OkHttp {@link Response} body is always closed via try-with-resources
 * so that the connection is returned to the pool even on error paths.
 */
public final class HttpMetricSender implements MetricSender {

    private static final MediaType JSON_MEDIA_TYPE =
            MediaType.parse("application/json; charset=utf-8");

    private static final String HEADER_CONTENT_TYPE = "Content-Type";
    private static final String HEADER_AGENT_ID     = "X-Agent-Id";

    private final String       endpoint;
    private final OkHttpClient httpClient;
    private final ObjectMapper objectMapper;
    private final RetryPolicy  retryPolicy;

    /**
     * Constructs an {@code HttpMetricSender} from the supplied configuration.
     *
     * @param config validated agent configuration; must not be {@code null}
     */
    public HttpMetricSender(AgentConfig config) {
        this.endpoint = config.getCollectorEndpoint();
        this.httpClient = buildHttpClient();
        this.objectMapper = buildObjectMapper();
        this.retryPolicy = new RetryPolicy(config.getRetryAttempts(), config.getRetryDelayMs());
    }

    // -------------------------------------------------------------------------
    // MetricSender
    // -------------------------------------------------------------------------

    /**
     * Serialises {@code payload} to JSON and POSTs it to the collector endpoint.
     *
     * <p>This method never throws; all failure modes are logged and swallowed.
     *
     * @param payload the batch to forward; must not be {@code null}
     */
    @Override
    public void send(MetricPayload payload) {
        try {
            final String json = objectMapper.writeValueAsString(payload);
            final String agentId = payload.metadata().agentId();

            retryPolicy.execute(() -> {
                RequestBody body = RequestBody.create(json, JSON_MEDIA_TYPE);

                Request request = new Request.Builder()
                        .url(endpoint)
                        .post(body)
                        .header(HEADER_CONTENT_TYPE, "application/json")
                        .header(HEADER_AGENT_ID, agentId)
                        .build();

                // try-with-resources ensures the response body is always closed,
                // returning the underlying connection to the OkHttp connection pool.
                try (Response response = httpClient.newCall(request).execute()) {
                    int code = response.code();
                    if (code >= 400) {
                        AgentLogger.warn(
                                "db-monitor-agent: collector returned HTTP " + code
                                + " for endpoint " + endpoint
                                + " — batch of " + payload.events().size() + " event(s) lost"
                        );
                    } else {
                        AgentLogger.debug(
                                "db-monitor-agent: successfully sent batch of "
                                + payload.events().size() + " event(s), HTTP " + code
                        );
                    }
                }
                return null;
            });

        } catch (IOException e) {
            // Thrown by OkHttp when the network is unavailable or the connection
            // is refused.  Logged as a warning because these are usually transient.
            AgentLogger.warn(
                    "db-monitor-agent: IOException sending metrics to " + endpoint
                    + " — " + e.getMessage()
            );
        } catch (Exception e) {
            // Any other exception (serialisation failure, malformed URL, etc.)
            // is logged at ERROR level because it typically indicates a bug.
            AgentLogger.error(
                    "db-monitor-agent: unexpected error sending metrics to " + endpoint, e
            );
        }
    }

    // -------------------------------------------------------------------------
    // Construction helpers
    // -------------------------------------------------------------------------

    private static OkHttpClient buildHttpClient() {
        return new OkHttpClient.Builder()
                .connectTimeout(5,  TimeUnit.SECONDS)
                .writeTimeout(10,   TimeUnit.SECONDS)
                .readTimeout(10,    TimeUnit.SECONDS)
                .build();
    }

    private static ObjectMapper buildObjectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        // Prevent exceptions when serialising Java records or POJOs that
        // expose no bean properties (e.g. empty event lists).
        mapper.disable(SerializationFeature.FAIL_ON_EMPTY_BEANS);
        return mapper;
    }
}
