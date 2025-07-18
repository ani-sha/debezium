/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.snapshot.mode;

/**
 * @deprecated to be removed in Debezium 3.0, replaced by {{@link RecoverySnapshotter}}
 */
public class SchemaOnlyRecoverySnapshotter extends NoDataSnapshotter {

    @Override
    public String name() {
        return "schema_only_recovery";
    }

    @Override
    public boolean shouldStreamEventsStartingFromSnapshot() {
        return false;
    }
}
