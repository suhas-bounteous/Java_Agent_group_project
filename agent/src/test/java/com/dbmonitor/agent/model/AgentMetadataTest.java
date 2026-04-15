package com.dbmonitor.agent.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AgentMetadataTest {

    private static final String APP_NAME    = "my-service";
    private static final String ENVIRONMENT = "test";

    // -------------------------------------------------------------------------
    // create() factory
    // -------------------------------------------------------------------------

    @Test
    void create_setsAppName() {
        AgentMetadata metadata = AgentMetadata.create(APP_NAME, ENVIRONMENT);
        assertEquals(APP_NAME, metadata.appName(), "appName must equal the value passed to create()");
    }

    @Test
    void create_setsEnvironment() {
        AgentMetadata metadata = AgentMetadata.create(APP_NAME, ENVIRONMENT);
        assertEquals(ENVIRONMENT, metadata.environment(), "environment must equal the value passed to create()");
    }

    @Test
    void create_generatesNonNullAgentId() {
        AgentMetadata metadata = AgentMetadata.create(APP_NAME, ENVIRONMENT);
        assertNotNull(metadata.agentId(), "agentId must not be null");
        assertFalse(metadata.agentId().isBlank(), "agentId must not be blank");
    }

    @Test
    void create_agentIdIsValidUuid() {
        AgentMetadata metadata = AgentMetadata.create(APP_NAME, ENVIRONMENT);
        // A randomly generated UUID has the canonical 8-4-4-4-12 hex format.
        assertTrue(
                metadata.agentId().matches(
                        "[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}"
                ),
                "agentId must be a canonical UUID string"
        );
    }

    @Test
    void create_twoCallsProduceDifferentAgentIds() {
        AgentMetadata first  = AgentMetadata.create(APP_NAME, ENVIRONMENT);
        AgentMetadata second = AgentMetadata.create(APP_NAME, ENVIRONMENT);
        assertNotEquals(
                first.agentId(),
                second.agentId(),
                "Each call to create() must generate a distinct agentId"
        );
    }

    @Test
    void create_startupTimeIsRecent() {
        long before = System.currentTimeMillis();
        AgentMetadata metadata = AgentMetadata.create(APP_NAME, ENVIRONMENT);
        long after = System.currentTimeMillis();

        assertTrue(
                metadata.startupTimeEpochMs() >= before && metadata.startupTimeEpochMs() <= after + 1000,
                "startupTimeEpochMs must be within 1000 ms of System.currentTimeMillis() at creation time"
        );
    }

    @Test
    void create_setsAgentVersion() {
        AgentMetadata metadata = AgentMetadata.create(APP_NAME, ENVIRONMENT);
        assertEquals(
                AgentMetadata.AGENT_VERSION,
                metadata.agentVersion(),
                "agentVersion must equal the compile-time constant"
        );
    }

    @Test
    void create_setsNonNullHostName() {
        AgentMetadata metadata = AgentMetadata.create(APP_NAME, ENVIRONMENT);
        assertNotNull(metadata.hostName(), "hostName must never be null (falls back to 'unknown')");
        assertFalse(metadata.hostName().isBlank(), "hostName must not be blank");
    }

    @Test
    void create_setsNonNullJvmId() {
        AgentMetadata metadata = AgentMetadata.create(APP_NAME, ENVIRONMENT);
        assertNotNull(metadata.jvmId(), "jvmId must not be null");
        assertFalse(metadata.jvmId().isBlank(), "jvmId must not be blank");
    }
}
