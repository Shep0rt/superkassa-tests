package kz.superkassa.tests.framework.protocol

import java.time.Instant

@Suppress("unused")
data class CpcrMessageArtifact(
    val correlationId: String,
    val command: String,
    val version: String,
    val rawPayload: ByteArray,
    val createdAt: Instant,
)
