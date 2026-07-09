package kz.superkassa.tests.framework.support

import java.time.Instant
import java.time.format.DateTimeFormatter
import java.util.UUID

@Suppress("unused")
class TestRun private constructor(
    val id: String,
) {
    companion object {
        fun create(): TestRun {
            val timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now())
                .replace(":", "")
                .replace(".", "")
            return TestRun("sk-$timestamp-${UUID.randomUUID()}")
        }
    }
}
