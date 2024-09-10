/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.pipeline;

import java.util.List;
import java.util.Map;

import io.debezium.config.CommonConnectorConfig;
import io.debezium.pipeline.spi.ChangeOffsetHandler;
import io.debezium.pipeline.spi.OffsetContext;

public class CommonChangeOffsetHandler<O extends OffsetContext> implements ChangeOffsetHandler<O> {

    public CommonChangeOffsetHandler(CommonConnectorConfig config) {
    }

    public List<String> getRequiredFields() {
        return List.of();
    }

    public O load(Map<String, ?> data) {
        return null;
    }

    public void store(O offsetContext) {
    }

    public String validate(Map<String, ?> data) {
        return null;
    }
}
