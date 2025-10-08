package org.haven.reporting.domain;

import org.haven.shared.Identifier;
import java.util.UUID;

/**
 * Value object for ExportJob aggregate identifier
 */
public class ExportJobId extends Identifier {

    public ExportJobId(UUID value) {
        super(value);
        if (value == null) {
            throw new IllegalArgumentException("ExportJobId cannot be null");
        }
    }

    public static ExportJobId generate() {
        return new ExportJobId(UUID.randomUUID());
    }

    public static ExportJobId of(UUID value) {
        return new ExportJobId(value);
    }
}
