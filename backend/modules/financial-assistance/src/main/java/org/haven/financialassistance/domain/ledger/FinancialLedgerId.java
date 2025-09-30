package org.haven.financialassistance.domain.ledger;

import org.haven.shared.Identifier;
import java.util.UUID;

public class FinancialLedgerId extends Identifier {

    private FinancialLedgerId(UUID value) {
        super(value);
    }

    public static FinancialLedgerId generate() {
        return new FinancialLedgerId(UUID.randomUUID());
    }

    public static FinancialLedgerId of(UUID value) {
        return new FinancialLedgerId(value);
    }

    public static FinancialLedgerId of(String value) {
        return new FinancialLedgerId(UUID.fromString(value));
    }
}