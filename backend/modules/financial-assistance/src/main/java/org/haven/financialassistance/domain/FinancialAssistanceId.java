package org.haven.financialassistance.domain;

import org.haven.shared.Identifier;
import java.util.UUID;

public class FinancialAssistanceId extends Identifier {
    
    public FinancialAssistanceId(UUID value) {
        super(value);
    }
    
    public static FinancialAssistanceId generate() {
        return new FinancialAssistanceId(UUID.randomUUID());
    }
    
    public static FinancialAssistanceId of(UUID value) {
        return new FinancialAssistanceId(value);
    }
}