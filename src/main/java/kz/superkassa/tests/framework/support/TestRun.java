package ru.superkassa.tests.framework.support;

import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

public final class TestRun {
    private final String id;

    private TestRun(String id) {
        this.id = id;
    }

    public static TestRun create() {
        String timestamp = DateTimeFormatter.ISO_INSTANT.format(Instant.now()).replace(":", "").replace(".", "");
        return new TestRun("sk-" + timestamp + "-" + UUID.randomUUID());
    }

    public String id() {
        return id;
    }
}
