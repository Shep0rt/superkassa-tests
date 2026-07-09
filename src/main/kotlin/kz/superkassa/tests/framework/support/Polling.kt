package kz.superkassa.tests.framework.support

import kz.superkassa.tests.framework.config.TestConfig
import org.awaitility.Awaitility
import java.util.concurrent.Callable

@Suppress("unused")
class Polling(
    private val config: TestConfig,
) {
    fun <T> until(supplier: Callable<T>): T =
        Awaitility.await()
            .atMost(config.defaultTimeout)
            .pollInterval(config.pollingInterval)
            .until(supplier) { it != null }
}
