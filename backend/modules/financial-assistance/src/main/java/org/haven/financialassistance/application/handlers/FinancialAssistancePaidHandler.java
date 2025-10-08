package org.haven.financialassistance.application.handlers;

import org.haven.financialassistance.domain.events.FinancialAssistancePaid;
import org.haven.shared.events.EventHandler;
import org.springframework.stereotype.Component;

/**
 * Handles financial assistance payment events for accounting and compliance
 */
@Component
public class FinancialAssistancePaidHandler implements EventHandler<FinancialAssistancePaid> {

    @Override
    public void handle(FinancialAssistancePaid event) {
        System.out.println("Financial assistance paid for client: " + event.clientId() + 
                          " - Type: " + event.assistanceType().toString() +
                          " - Amount paid: $" + event.paidAmount() +
                          " - Payment method: " + event.paymentMethod() +
                          " - Payment date: " + event.paymentDate() +
                          " - Paid by: " + event.paidBy());
        
        // Financial accountability and reporting workflows:
        // - Update accounting systems with payment details
        // - Generate payment confirmation documentation
        // - Update client assistance history
        // - Track against program budgets and limits
        // - Generate compliance reports for funders
        // - Document payment method and reference numbers
        
        if (event.isPartialPayment() && event.remainingBalance() != null) {
            System.out.println("PARTIAL PAYMENT: Remaining balance $" + event.remainingBalance());
            // - Schedule follow-up payment if needed
            // - Update assistance request status
            // - Notify client of remaining balance
        } else {
            System.out.println("FULL PAYMENT: Assistance request completed");
            // - Mark assistance request as fulfilled
            // - Generate completion notification
        }
        
        // Payment tracking and audit trail
        if (event.checkNumber() != null && !event.checkNumber().trim().isEmpty()) {
            System.out.println("Check number: " + event.checkNumber());
        }
        
        if (event.paymentReference() != null && !event.paymentReference().trim().isEmpty()) {
            System.out.println("Payment reference: " + event.paymentReference());
        }
        
        // Special handling notes
        if (event.paymentNotes() != null && !event.paymentNotes().trim().isEmpty()) {
            System.out.println("Payment notes: " + event.paymentNotes());
        }
        
        // Verification of payment amount
        System.out.println("Approved: $" + event.approvedAmount() + " - Paid: $" + event.paidAmount());
        if (!event.approvedAmount().equals(event.paidAmount())) {
            System.out.println("NOTICE: Paid amount differs from approved amount - review required");
        }
    }

    @Override
    public Class<FinancialAssistancePaid> getEventType() {
        return FinancialAssistancePaid.class;
    }
}