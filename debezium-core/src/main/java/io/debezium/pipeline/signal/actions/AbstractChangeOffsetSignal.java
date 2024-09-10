/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.pipeline.signal.actions;

import io.debezium.pipeline.signal.SignalPayload;
import io.debezium.pipeline.spi.Partition;

public class AbstractChangeOffsetSignal<P extends Partition> implements SignalAction<P> {
    public static final String FIELD_CONNECTOR_OFFSETS = "connector-offsets";

    @Override
    public boolean arrived(SignalPayload<P> signalPayload) throws InterruptedException {
        return true;
    }
}
