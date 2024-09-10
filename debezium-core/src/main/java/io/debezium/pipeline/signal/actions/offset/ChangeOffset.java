/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.pipeline.signal.actions.offset;

import java.util.HashMap;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.debezium.document.Array;
import io.debezium.document.Document;
import io.debezium.pipeline.ChangeEventSourceCoordinator;
import io.debezium.pipeline.EventDispatcher;
import io.debezium.pipeline.signal.SignalPayload;
import io.debezium.pipeline.signal.actions.AbstractChangeOffsetSignal;
import io.debezium.pipeline.spi.OffsetContext;
import io.debezium.pipeline.spi.Partition;

public class ChangeOffset<P extends Partition> extends AbstractChangeOffsetSignal<P> {
    private static final Logger LOGGER = LoggerFactory.getLogger(ChangeOffset.class);
    public static final String NAME = "change-offset";
    private final EventDispatcher<P, ?> dispatcher;
    private final ChangeEventSourceCoordinator<P, ? extends OffsetContext> changeEventSourceCoordinator;

    public ChangeOffset(EventDispatcher<P, ?> dispatcher, ChangeEventSourceCoordinator<P, ? extends OffsetContext> changeEventSourceCoordinator) {
        this.dispatcher = dispatcher;
        this.changeEventSourceCoordinator = changeEventSourceCoordinator;
    }

    @Override
    public boolean arrived(SignalPayload<P> signalPayload) throws InterruptedException {
        LOGGER.debug("Requested {} with offset data {}", NAME, signalPayload.data);

        final Map<String, ?> data = getOffsetData(signalPayload.data);
        if (data.isEmpty()) {
            return false;
        }

        changeEventSourceCoordinator.doChangeOffset(signalPayload.partition, signalPayload.offsetContext, data);
        return true;
    }

    private Map<String, ?> getOffsetData(Document data) {
        Map<String, Object> offsetData = new HashMap<>();
        if (data.isEmpty()) {
            throw new IllegalArgumentException("Offset data must not be empty");
        }

        // handling single offset data now
        Array offsetsDataArray = data.getArray(FIELD_CONNECTOR_OFFSETS);
        for (int i = 0; i < offsetsDataArray.size(); i++) {
            Document offsetDataDoc = offsetsDataArray.get(i).asDocument();
            for (CharSequence offsetKey : offsetDataDoc.keySet()) {
                offsetData.put(offsetKey.toString(), offsetDataDoc.get(offsetKey).asObject());
            }
        }
        return offsetData;
    }
}
