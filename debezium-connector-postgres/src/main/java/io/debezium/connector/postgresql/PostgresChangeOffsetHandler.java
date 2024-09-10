/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.connector.postgresql;

import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.debezium.connector.postgresql.connection.Lsn;
import io.debezium.pipeline.CommonChangeOffsetHandler;
import io.debezium.pipeline.txmetadata.TransactionContext;

public class PostgresChangeOffsetHandler extends CommonChangeOffsetHandler<PostgresOffsetContext> {
    private static final Logger LOGGER = LoggerFactory.getLogger(PostgresChangeOffsetHandler.class);
    private final PostgresConnectorConfig config;
    private static final String FIELD_LSN = "lsn";
    private static final String FIELD_LAST_PROCESSED_LSN = "lsn_proc";
    private static final String FIELD_LAST_COMMITTED_LSN = "lsn_commit";

    public PostgresChangeOffsetHandler(PostgresConnectorConfig config) {
        super(config);
        this.config = config;
    }

    @Override
    public PostgresOffsetContext load(Map<String, ?> data) {
        if (data == null) {
            return null;
        }

        final Lsn lsn = Lsn.valueOf(data.get(FIELD_LSN).toString());
        final Lsn lastCompletelyProcessedLsn = Lsn.valueOf(data.get(FIELD_LAST_PROCESSED_LSN).toString());
        Lsn lastCommitLsn = Lsn.valueOf(data.get(FIELD_LAST_COMMITTED_LSN).toString());
        if (lastCommitLsn == null) {
            lastCommitLsn = lastCompletelyProcessedLsn;
        }

        return new PostgresOffsetContext(
                config,
                lsn,
                lastCompletelyProcessedLsn,
                lastCommitLsn,
                null,
                null,
                Instant.now(),
                false,
                false,
                new TransactionContext(),
                new PostgresReadOnlyIncrementalSnapshotContext<>());
    }

    @Override
    public String validate(Map<String, ?> data) {
        if (data == null) {
            return "Processing of change offset signal failed: required fields " + getRequiredFields();
        }
        return null;
    }

    @Override
    public List<String> getRequiredFields() {
        return List.of(FIELD_LSN, FIELD_LAST_PROCESSED_LSN, FIELD_LAST_COMMITTED_LSN);
    }
}
