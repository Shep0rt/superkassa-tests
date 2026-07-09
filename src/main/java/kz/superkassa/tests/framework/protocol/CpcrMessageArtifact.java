package ru.superkassa.tests.framework.protocol;

import java.time.Instant;

public record CpcrMessageArtifact(
        String correlationId,
        String command,
        String version,
        byte[] rawPayload,
        Instant createdAt
) {
}
