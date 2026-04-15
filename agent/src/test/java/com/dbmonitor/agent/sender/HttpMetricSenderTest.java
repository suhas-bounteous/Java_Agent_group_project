package com.dbmonitor.agent.sender;

import com.dbmonitor.agent.config.AgentConfig;
import com.dbmonitor.agent.config.MaskingLevel;
import com.dbmonitor.agent.model.AgentMetadata;
import com.dbmonitor.agent.model.DbEvent;
import com.dbmonitor.agent.model.EventType;
import com.dbmonitor.agent.model.MetricPayload;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

/**
 * Integration tests for {@link HttpMetricSender} against a real (WireMock) HTTP
 * server on port 8081.
 *
 * <p>{@code @WireMockTest} starts a WireMock server before each test and stops
 * it afterwards.  All stub registrations are reset between tests automatically.
 */
@WireMockTest(httpPort = 8081)
class HttpMetricSenderTest {

    private static final String EVENTS_PATH = "/api/events";

    private AgentConfig    config;
    private AgentMetadata  metadata;

    @BeforeEach
    void setUp() {
        config = AgentConfig.builder()
                .appName("http-sender-test")
                .environment("test")
                .collectorEndpoint("http://localhost:8081" + EVENTS_PATH)
                .reportingIntervalMs(5_000L)
                .slowQueryThresholdMs(1_000L)
                .sqlCapture(MaskingLevel.FULL)
                .maxQueueSize(1_000)
                .retryAttempts(1)   // 1 total attempt — keeps tests fast
                .retryDelayMs(0L)
                .build();

        metadata = AgentMetadata.create("http-sender-test", "test");
    }

    // -------------------------------------------------------------------------
    // send_postsJsonToCollectorEndpoint
    // -------------------------------------------------------------------------

    @Test
    void send_postsJsonToCollectorEndpoint() {
        stubFor(post(urlEqualTo(EVENTS_PATH))
                .willReturn(aResponse().withStatus(200)));

        MetricPayload payload = MetricPayload.of(metadata, List.of());
        new HttpMetricSender(config).send(payload);

        verify(postRequestedFor(urlEqualTo(EVENTS_PATH)));
    }

    // -------------------------------------------------------------------------
    // send_includesAgentIdHeader
    // -------------------------------------------------------------------------

    @Test
    void send_includesAgentIdHeader() {
        stubFor(post(urlEqualTo(EVENTS_PATH))
                .willReturn(aResponse().withStatus(200)));

        MetricPayload payload = MetricPayload.of(metadata, List.of());
        new HttpMetricSender(config).send(payload);

        // The X-Agent-Id value must match the agentId of the payload's metadata.
        verify(postRequestedFor(urlEqualTo(EVENTS_PATH))
                .withHeader("X-Agent-Id", equalTo(payload.metadata().agentId())));
    }

    // -------------------------------------------------------------------------
    // send_doesNotThrow_whenServerReturns500
    // -------------------------------------------------------------------------

    @Test
    void send_doesNotThrow_whenServerReturns500() {
        stubFor(post(urlEqualTo(EVENTS_PATH))
                .willReturn(aResponse().withStatus(500).withBody("Internal Server Error")));

        MetricPayload payload = MetricPayload.of(metadata, List.of());
        HttpMetricSender sender = new HttpMetricSender(config);

        assertDoesNotThrow(() -> sender.send(payload),
                "send() must not throw when the server returns a 5xx status");
    }

    @Test
    void send_doesNotThrow_whenServerReturns400() {
        stubFor(post(urlEqualTo(EVENTS_PATH))
                .willReturn(aResponse().withStatus(400).withBody("Bad Request")));

        MetricPayload payload = MetricPayload.of(metadata, List.of());

        assertDoesNotThrow(() -> new HttpMetricSender(config).send(payload),
                "send() must not throw when the server returns a 4xx status");
    }

    // -------------------------------------------------------------------------
    // send_doesNotThrow_whenServerIsDown
    // -------------------------------------------------------------------------

    /**
     * Uses a port ({@code 19876}) that has nothing listening so OkHttp receives
     * a connection-refused error.  The {@link AgentConfig} for this test is
     * intentionally different from the one pointing at WireMock (8081).
     */
    @Test
    void send_doesNotThrow_whenServerIsDown() {
        // Build a config that points at a port with nothing listening.
        // retryAttempts=1 + retryDelayMs=0 keeps the test sub-second.
        AgentConfig deadConfig = AgentConfig.builder()
                .appName("http-sender-test")
                .environment("test")
                .collectorEndpoint("http://localhost:19876" + EVENTS_PATH)
                .reportingIntervalMs(5_000L)
                .slowQueryThresholdMs(1_000L)
                .sqlCapture(MaskingLevel.FULL)
                .maxQueueSize(1_000)
                .retryAttempts(1)
                .retryDelayMs(0L)
                .build();

        MetricPayload payload = MetricPayload.of(metadata, List.of());
        HttpMetricSender sender = new HttpMetricSender(deadConfig);

        assertDoesNotThrow(() -> sender.send(payload),
                "send() must not throw when the collector server is unreachable");
    }

    // -------------------------------------------------------------------------
    // send_payloadIsValidJson
    // -------------------------------------------------------------------------

    @Test
    void send_payloadIsValidJson() {
        stubFor(post(urlEqualTo(EVENTS_PATH))
                .willReturn(aResponse().withStatus(200)));

        DbEvent event = DbEvent.success(
                EventType.QUERY_EXECUTE, "SELECT 1", 10L, "jdbc:h2:mem:test");

        MetricPayload payload = MetricPayload.of(metadata, List.of(event));
        new HttpMetricSender(config).send(payload);

        // Verify that the request carried the correct Content-Type header and
        // that the body is valid JSON containing the expected top-level keys.
        verify(postRequestedFor(urlEqualTo(EVENTS_PATH))
                .withHeader("Content-Type", containing("application/json"))
                .withRequestBody(matchingJsonPath("$.metadata"))
                .withRequestBody(matchingJsonPath("$.events"))
                .withRequestBody(matchingJsonPath("$.batchTimestampEpochMs")));
    }

    @Test
    void send_payloadEvents_containsEventFields(WireMockRuntimeInfo wmRuntimeInfo) {
        stubFor(post(urlEqualTo(EVENTS_PATH))
                .willReturn(aResponse().withStatus(200)));

        DbEvent event = DbEvent.success(
                EventType.QUERY_EXECUTE, "SELECT 1", 42L, "jdbc:h2:mem:test");

        MetricPayload payload = MetricPayload.of(metadata, List.of(event));
        new HttpMetricSender(config).send(payload);

        // The serialised events array must contain at least one element with the
        // fields we set above.
        verify(postRequestedFor(urlEqualTo(EVENTS_PATH))
                .withRequestBody(matchingJsonPath("$.events[0].eventType"))
                .withRequestBody(matchingJsonPath("$.events[0].durationMs"))
                .withRequestBody(matchingJsonPath("$.events[0].success")));
    }

    @Test
    void send_metadata_containsAgentId(WireMockRuntimeInfo wmRuntimeInfo) {
        stubFor(post(urlEqualTo(EVENTS_PATH))
                .willReturn(aResponse().withStatus(200)));

        MetricPayload payload = MetricPayload.of(metadata, List.of());
        new HttpMetricSender(config).send(payload);

        verify(postRequestedFor(urlEqualTo(EVENTS_PATH))
                .withRequestBody(matchingJsonPath("$.metadata.agentId"))
                .withRequestBody(matchingJsonPath("$.metadata.appName")));
    }
}
