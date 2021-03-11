/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.server;

import org.testcontainers.containers.GenericContainer;
import org.testcontainers.utility.DockerImageName;

public class DebeziumServerContainer extends GenericContainer<DebeziumServerContainer> {

    private static final String DEBEZIUM_VERSION = "1.4";

    public DebeziumServerContainer(final String containerImageName) {
        super(DockerImageName.parse(containerImageName));
        defaultConfig();
    }

    private void defaultConfig() {
        withEnv("debezium.sink.type", "test");
        withEnv("debezium.source.connector.class", "io.debezium.connector.postgresql.PostgresConnector");
        withEnv("debezium.source.offset.flush.interval.ms", "0");
        withEnv("debezium.source.database.server.name", "testc");
        withEnv("debezium.source.schema.include.list", "inventory");
        withEnv("debezium.source.table.include.list", "inventory.customers");
    }

    public static DebeziumServerContainer latestStable() {
        return new DebeziumServerContainer("debezium/server:" + DEBEZIUM_VERSION);
    }
}
