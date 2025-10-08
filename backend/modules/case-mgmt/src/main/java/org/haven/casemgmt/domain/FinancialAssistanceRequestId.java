package org.haven.casemgmt.domain;

import org.haven.shared.Identifier;
import java.util.UUID;

public class FinancialAssistanceRequestId extends Identifier {
    
    public FinancialAssistanceRequestId(UUID value) {
        super(value);
    }
    
    public static FinancialAssistanceRequestId generate() {
        return new FinancialAssistanceRequestId(UUID.randomUUID());
    }
    
    public static FinancialAssistanceRequestId of(UUID value) {
        return new FinancialAssistanceRequestId(value);
    }
}