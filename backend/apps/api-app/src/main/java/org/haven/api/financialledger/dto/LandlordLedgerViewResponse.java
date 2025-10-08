package org.haven.api.financialledger.dto;

import org.haven.financialassistance.application.services.VawaLedgerRedactionService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record LandlordLedgerViewResponse(
    UUID ledgerId,
    UUID clientId, // May be null for VAWA protected
    String clientName, // May be redacted
    String landlordId,
    List<LedgerEntryResponse> visibleEntries,
    BigDecimal visibleBalance,
    boolean isVawaProtected,
    int visibleTransactionCount,
    BigDecimal visiblePaymentTotal
) {
    public static LandlordLedgerViewResponse fromDomain(VawaLedgerRedactionService.LedgerLandlordView view) {
        List<LedgerEntryResponse> entryResponses = view.getVisibleEntries().stream()
            .map(LedgerEntryResponse::fromDomain)
            .toList();

        return new LandlordLedgerViewResponse(
            view.getLedgerId(),
            view.getClientId(),
            view.getClientName(),
            view.getLandlordId(),
            entryResponses,
            view.getVisibleBalance(),
            view.isVawaProtected(),
            view.getVisibleTransactionCount(),
            view.getVisiblePaymentTotal()
        );
    }
}