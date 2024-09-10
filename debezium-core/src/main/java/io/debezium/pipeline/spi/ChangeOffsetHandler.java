/*
 * Copyright Debezium Authors.
 *
 * Licensed under the Apache Software License version 2.0, available at http://www.apache.org/licenses/LICENSE-2.0
 */
package io.debezium.pipeline.spi;

import java.util.List;
import java.util.Map;

public interface ChangeOffsetHandler<O extends OffsetContext> {

    O load(Map<String, ?> data);

    void store(O offsetContext);

    String validate(Map<String, ?> data);

    List<String> getRequiredFields();
}
