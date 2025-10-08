package org.haven.casemgmt.domain.mandatedreport;

import org.haven.shared.Identifier;
import java.util.UUID;

/**
 * Value object for MandatedReport aggregate identifier
 */
public class MandatedReportId extends Identifier {
    
    public MandatedReportId(UUID value) {
        super(value);
    }
    
    public static MandatedReportId newId() {
        return new MandatedReportId(UUID.randomUUID());
    }
    
    public static MandatedReportId fromString(String id) {
        return new MandatedReportId(UUID.fromString(id));
    }
}