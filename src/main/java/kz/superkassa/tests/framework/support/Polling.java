package ru.superkassa.tests.framework.support;

import org.awaitility.Awaitility;
import ru.superkassa.tests.framework.config.TestConfig;

import java.util.concurrent.Callable;

public final class Polling {
    private final TestConfig config;

    public Polling(TestConfig config) {
        this.config = config;
    }

    public <T> T until(Callable<T> supplier) {
        return Awaitility.await()
                .atMost(config.defaultTimeout())
                .pollInterval(config.pollingInterval())
                .until(supplier, value -> value != null);
    }
}
