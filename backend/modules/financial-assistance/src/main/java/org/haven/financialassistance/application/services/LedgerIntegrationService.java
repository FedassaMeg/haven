package org.haven.financialassistance.application.services;

import org.haven.clientprofile.domain.ClientId;
import org.haven.financialassistance.domain.FinancialAssistance;
import org.haven.financialassistance.domain.ledger.*;
import org.haven.programenrollment.domain.ProgramEnrollmentId;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Service responsible for integrating ledger writes with existing payment workflows.
 * Listens to payment events and automatically records them in the appropriate ledgers.
 */
@Service
@Transactional
public class LedgerIntegrationService {

    private final FinancialLedgerService ledgerService;

    public LedgerIntegrationService(FinancialLedgerService ledgerService) {
        this.ledgerService = ledgerService;
    }

    /**
     * Handle financial assistance payment authorization events
     */
    @EventListener
    public void handleFinancialAssistancePayment(FinancialAssistancePaymentEvent event) {
        try {
            FinancialAssistance.Payment payment = event.getPayment();
            FinancialAssistance assistance = event.getAssistance();

            // Get or create active ledger for the client
            FinancialLedger ledger = ledgerService.getOrCreateActiveLedger(
                assistance.getClientId(),
                assistance.getEnrollmentId(),
                generateHouseholdId(assistance.getClientId()), // TODO: Get actual household ID
                isVawaProtected(assistance.getClientId()),
                "SYSTEM"
            );

            // Map payment subtype to ledger payment subtype
            PaymentSubtype ledgerSubtype = mapToLedgerSubtype(payment.getSubtype());

            // Record the payment in the ledger
            ledgerService.recordPaymentTransaction(
                ledger.getId(),
                payment.getPaymentId().toString(),
                assistance.getId().value().toString(),
                payment.getAmount(),
                assistance.getFundingSourceCode(),
                getHudCategoryCode(payment.getSubtype()),
                ledgerSubtype,
                payment.getPayeeId(),
                payment.getPayeeName(),
                payment.getPaymentDate(),
                payment.getPeriodStart(),
                payment.getPeriodEnd(),
                payment.getAuthorizedBy()
            );

        } catch (Exception e) {
            // Log error but don't fail the payment process
            System.err.println("Failed to record payment in ledger: " + e.getMessage());
        }
    }

    /**
     * Record funding deposit when grants are received
     */
    public void recordFundingDeposit(ClientId clientId, ProgramEnrollmentId enrollmentId,
                                   String depositId, java.math.BigDecimal amount, String fundingSourceCode,
                                   String depositSource, LocalDate depositDate, String recordedBy) {
        try {
            FinancialLedger ledger = ledgerService.getOrCreateActiveLedger(
                clientId,
                enrollmentId,
                generateHouseholdId(clientId),
                isVawaProtected(clientId),
                recordedBy
            );

            ledgerService.recordFundingDeposit(
                ledger.getId(),
                depositId,
                amount,
                fundingSourceCode,
                depositSource,
                depositDate,
                recordedBy
            );

        } catch (Exception e) {
            System.err.println("Failed to record funding deposit in ledger: " + e.getMessage());
        }
    }

    /**
     * Record arrears when identified
     */
    public void recordClientArrears(ClientId clientId, ProgramEnrollmentId enrollmentId,
                                  String arrearsId, java.math.BigDecimal amount, ArrearsType arrearsType,
                                  String payeeId, String payeeName, LocalDate periodStart, LocalDate periodEnd,
                                  String recordedBy) {
        try {
            FinancialLedger ledger = ledgerService.getOrCreateActiveLedger(
                clientId,
                enrollmentId,
                generateHouseholdId(clientId),
                isVawaProtected(clientId),
                recordedBy
            );

            ledgerService.recordArrears(
                ledger.getId(),
                arrearsId,
                amount,
                arrearsType,
                payeeId,
                payeeName,
                periodStart,
                periodEnd,
                recordedBy
            );

        } catch (Exception e) {
            System.err.println("Failed to record arrears in ledger: " + e.getMessage());
        }
    }

    private PaymentSubtype mapToLedgerSubtype(FinancialAssistance.AssistancePaymentSubtype subtype) {
        return switch (subtype) {
            case RENT_CURRENT -> PaymentSubtype.RENT_CURRENT;
            case RENT_ARREARS -> PaymentSubtype.RENT_ARREARS;
            case UTILITY_CURRENT -> PaymentSubtype.UTILITY_CURRENT;
            case UTILITY_ARREARS -> PaymentSubtype.UTILITY_ARREARS;
            case SECURITY_DEPOSIT -> PaymentSubtype.SECURITY_DEPOSIT;
            case MOVING_COSTS -> PaymentSubtype.MOVING_COSTS;
            case OTHER -> PaymentSubtype.OTHER;
        };
    }

    private String getHudCategoryCode(FinancialAssistance.AssistancePaymentSubtype subtype) {
        return switch (subtype) {
            case RENT_CURRENT, RENT_ARREARS -> "4.02"; // Rental assistance
            case UTILITY_CURRENT, UTILITY_ARREARS -> "4.03"; // Utility assistance
            case SECURITY_DEPOSIT -> "4.04"; // Security deposits
            case MOVING_COSTS -> "4.05"; // Moving assistance
            default -> "4.99"; // Other assistance
        };
    }

    // TODO: Implement these methods based on your existing client profile system
    private UUID generateHouseholdId(ClientId clientId) {
        // For now, use client ID as household ID
        // In reality, this should lookup the actual household from the client profile
        return clientId.value();
    }

    private boolean isVawaProtected(ClientId clientId) {
        // TODO: Integrate with existing VAWA protection determination logic
        // This should check if the client has VAWA protections enabled
        return false; // Default to not protected for now
    }

    /**
     * Event wrapper for financial assistance payment events.
     * This allows the @EventListener to receive both payment and assistance information.
     */
    public static class FinancialAssistancePaymentEvent {
        private final FinancialAssistance.Payment payment;
        private final FinancialAssistance assistance;

        public FinancialAssistancePaymentEvent(FinancialAssistance.Payment payment, FinancialAssistance assistance) {
            this.payment = payment;
            this.assistance = assistance;
        }

        public FinancialAssistance.Payment getPayment() {
            return payment;
        }

        public FinancialAssistance getAssistance() {
            return assistance;
        }
    }
}
