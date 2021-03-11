/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.server;

import static io.debezium.server.TestConfigSource.OFFSETS_FILE;

import java.nio.file.Path;
import java.util.stream.Stream;

import org.apache.kafka.connect.runtime.standalone.StandaloneConfig;
import org.fest.assertions.Assertions;
import org.junit.BeforeClass;
import org.junit.Test;
import org.testcontainers.lifecycle.Startables;

import io.debezium.util.Testing;
import io.quarkus.test.junit.QuarkusTest;

/**
 * Smoke test that starts and checks the basic functionality of Quarkus-based Debezium Server.
 *
 * @author Anisha Mohanty
 */

@QuarkusTest
public class DebeziumServerContainerTest {
    public static final Path OFFSET_STORE_PATH = Testing.Files.createTestingPath(OFFSETS_FILE).toAbsolutePath();

    public static DebeziumServerContainer debeziumServerContainer = DebeziumServerContainer.latestStable()
            .withEnv("debezium.source." + StandaloneConfig.OFFSET_STORAGE_FILE_FILENAME_CONFIG, OFFSET_STORE_PATH.toAbsolutePath().toString());

    @BeforeClass
    public static void startContainers() {
        Startables.deepStart(Stream.of(debeziumServerContainer)).join();
    }

    @Test
    public void test() {
        Assertions.assertThat(debeziumServerContainer.isRunning());
    }

}
