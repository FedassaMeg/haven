package org.haven.financialassistance.domain.ledger;

import org.haven.clientprofile.domain.ClientId;
import org.haven.programenrollment.domain.ProgramEnrollmentId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Comprehensive tests for FinancialLedger domain aggregate
 */
class FinancialLedgerTest {

    private ClientId clientId;
    private ProgramEnrollmentId enrollmentId;
    private UUID householdId;
    private FinancialLedger ledger;

    @BeforeEach
    void setUp() {
        clientId = new ClientId(UUID.randomUUID());
        enrollmentId = new ProgramEnrollmentId(UUID.randomUUID());
        householdId = UUID.randomUUID();

        ledger = FinancialLedger.create(
            clientId,
            enrollmentId,
            householdId,
            "Test Financial Ledger",
            false, // Not VAWA protected
            "TEST_USER"
        );
    }

    @Test
    @DisplayName("Should create ledger with correct initial state")
    void shouldCreateLedgerWithCorrectInitialState() {
        assertNotNull(ledger.getId());
        assertEquals(clientId, ledger.getClientId());
        assertEquals(enrollmentId, ledger.getEnrollmentId());
        assertEquals(householdId, ledger.getHouseholdId());
        assertEquals("Test Financial Ledger", ledger.getLedgerName());
        assertEquals(LedgerStatus.ACTIVE, ledger.getStatus());
        assertFalse(ledger.isVawaProtected());
        assertEquals(BigDecimal.ZERO, ledger.getTotalDebits());
        assertEquals(BigDecimal.ZERO, ledger.getTotalCredits());
        assertEquals(BigDecimal.ZERO, ledger.getBalance());
        assertTrue(ledger.isBalanced());
        assertTrue(ledger.getEntries().isEmpty());
    }

    @Test
    @DisplayName("Should record payment transaction with double-entry accounting")
    void shouldRecordPaymentTransactionWithDoubleEntry() {
        // Record a rent payment
        BigDecimal paymentAmount = new BigDecimal("1500.00");
        ledger.recordPayment(
            "PAY_001",
            "ASSIST_001",
            paymentAmount,
            "HUD_ESG",
            "4.02",
            PaymentSubtype.RENT_CURRENT,
            "LANDLORD_001",
            "ABC Property Management",
            LocalDate.now(),
            null,
            null,
            "CASE_MANAGER"
        );

        // Verify double-entry accounting
        assertEquals(2, ledger.getEntries().size());
        assertEquals(paymentAmount, ledger.getTotalDebits());
        assertEquals(paymentAmount, ledger.getTotalCredits());
        assertEquals(BigDecimal.ZERO, ledger.getBalance());
        assertTrue(ledger.isBalanced());

        // Verify entry details
        boolean hasDebitEntry = ledger.getEntries().stream()
            .anyMatch(entry -> entry.getEntryType() == EntryType.DEBIT &&
                             entry.getAccountClassification() == AccountClassification.RENT_EXPENSE &&
                             entry.getAmount().equals(paymentAmount));

        boolean hasCreditEntry = ledger.getEntries().stream()
            .anyMatch(entry -> entry.getEntryType() == EntryType.CREDIT &&
                             entry.getAccountClassification() == AccountClassification.CASH_ASSET &&
                             entry.getAmount().equals(paymentAmount));

        assertTrue(hasDebitEntry, "Should have debit entry for rent expense");
        assertTrue(hasCreditEntry, "Should have credit entry for cash");
    }

    @Test
    @DisplayName("Should record funding deposit correctly")
    void shouldRecordFundingDeposit() {
        BigDecimal depositAmount = new BigDecimal("10000.00");
        ledger.recordDeposit(
            "DEP_001",
            depositAmount,
            "HUD_ESG",
            "Emergency Solutions Grant",
            LocalDate.now(),
            "FINANCIAL_ADMIN"
        );

        assertEquals(2, ledger.getEntries().size());
        assertEquals(depositAmount, ledger.getTotalDebits());
        assertEquals(depositAmount, ledger.getTotalCredits());
        assertTrue(ledger.isBalanced());

        // Verify funding deposit creates correct entries
        boolean hasDebitEntry = ledger.getEntries().stream()
            .anyMatch(entry -> entry.getEntryType() == EntryType.DEBIT &&
                             entry.getAccountClassification() == AccountClassification.CASH_ASSET);

        boolean hasCreditEntry = ledger.getEntries().stream()
            .anyMatch(entry -> entry.getEntryType() == EntryType.CREDIT &&
                             entry.getAccountClassification() == AccountClassification.FUNDING_LIABILITY);

        assertTrue(hasDebitEntry, "Should have debit entry for cash received");
        assertTrue(hasCreditEntry, "Should have credit entry for funding liability");
    }

    @Test
    @DisplayName("Should record arrears with proper validation")
    void shouldRecordArrearsWithProperValidation() {
        BigDecimal arrearsAmount = new BigDecimal("800.00");
        LocalDate periodStart = LocalDate.now().minusMonths(2);
        LocalDate periodEnd = LocalDate.now().minusMonths(1);

        ledger.recordArrears(
            "ARR_001",
            arrearsAmount,
            ArrearsType.RENT,
            "LANDLORD_001",
            "ABC Property Management",
            periodStart,
            periodEnd,
            "CASE_MANAGER"
        );

        assertEquals(2, ledger.getEntries().size());
        assertTrue(ledger.isBalanced());

        // Verify arrears entry has correct period information
        boolean hasArrearsEntry = ledger.getEntries().stream()
            .anyMatch(entry -> entry.getPeriodStart() != null &&
                             entry.getPeriodEnd() != null &&
                             entry.getPeriodStart().equals(periodStart) &&
                             entry.getPeriodEnd().equals(periodEnd));

        assertTrue(hasArrearsEntry, "Should have entry with period information");
    }

    @Test
    @DisplayName("Should handle multiple transactions while maintaining balance")
    void shouldHandleMultipleTransactionsWhileMaintainingBalance() {
        // Record funding deposit
        ledger.recordDeposit("DEP_001", new BigDecimal("5000.00"), "HUD_ESG", "Grant Funding", LocalDate.now(), "ADMIN");

        // Record rent payment
        ledger.recordPayment("PAY_001", "ASSIST_001", new BigDecimal("1200.00"), "HUD_ESG", "4.02",
                           PaymentSubtype.RENT_CURRENT, "LANDLORD_001", "Landlord A", LocalDate.now(), null, null, "CASE_MANAGER");

        // Record utility payment
        ledger.recordPayment("PAY_002", "ASSIST_002", new BigDecimal("300.00"), "HUD_ESG", "4.03",
                           PaymentSubtype.UTILITY_CURRENT, "UTILITY_001", "Electric Company", LocalDate.now(), null, null, "CASE_MANAGER");

        // Verify ledger remains balanced
        assertTrue(ledger.isBalanced());
        assertEquals(6, ledger.getEntries().size()); // 3 transactions × 2 entries each

        // Verify total amounts
        assertEquals(new BigDecimal("6500.00"), ledger.getTotalDebits());
        assertEquals(new BigDecimal("6500.00"), ledger.getTotalCredits());
    }

    @Test
    @DisplayName("Should record landlord communication")
    void shouldRecordLandlordCommunication() {
        ledger.recordLandlordCommunication(
            "COMM_001",
            "LANDLORD_001",
            "ABC Property Management",
            CommunicationType.EMAIL,
            "Payment Confirmation",
            "Rent payment of $1500 has been processed.",
            LocalDate.now(),
            "CASE_MANAGER"
        );

        // Communication doesn't affect ledger balance but should be recorded
        assertTrue(ledger.isBalanced());
        assertEquals(BigDecimal.ZERO, ledger.getTotalDebits());
        assertEquals(BigDecimal.ZERO, ledger.getTotalCredits());
    }

    @Test
    @DisplayName("Should attach documents")
    void shouldAttachDocuments() {
        byte[] documentContent = "Sample document content".getBytes();

        ledger.attachDocument(
            "DOC_001",
            "Receipt.pdf",
            "application/pdf",
            "CASE_MANAGER",
            documentContent
        );

        // Document attachment doesn't affect ledger balance
        assertTrue(ledger.isBalanced());
    }

    @Test
    @DisplayName("Should close ledger when balanced")
    void shouldCloseLedgerWhenBalanced() {
        // Add some transactions to make it interesting
        ledger.recordDeposit("DEP_001", new BigDecimal("1000.00"), "HUD_ESG", "Grant", LocalDate.now(), "ADMIN");
        ledger.recordPayment("PAY_001", "ASSIST_001", new BigDecimal("1000.00"), "HUD_ESG", "4.02",
                           PaymentSubtype.RENT_CURRENT, "LANDLORD_001", "Landlord", LocalDate.now(), null, null, "CASE_MANAGER");

        assertTrue(ledger.isBalanced());

        ledger.closeLedger("End of program period", "SUPERVISOR");

        assertEquals(LedgerStatus.CLOSED, ledger.getStatus());
    }

    @Test
    @DisplayName("Should throw exception when closing unbalanced ledger")
    void shouldThrowExceptionWhenClosingUnbalancedLedger() {
        // Create an artificially unbalanced state by only recording one side
        ledger.recordDeposit("DEP_001", new BigDecimal("1000.00"), "HUD_ESG", "Grant", LocalDate.now(), "ADMIN");
        // Manually corrupt balance for testing
        ledger.recordTransaction("CORRUPT_001", TransactionType.RENT_PAYMENT, new BigDecimal("500.00"),
                               "HUD_ESG", "4.02", "Corrupted entry", "LANDLORD_001", "Landlord",
                               null, null, "TEST");

        // This should create an imbalance
        assertFalse(ledger.isBalanced());

        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            ledger.closeLedger("Test closure", "SUPERVISOR");
        });

        assertTrue(exception.getMessage().contains("Cannot close unbalanced ledger"));
    }

    @Test
    @DisplayName("Should prevent recording transactions on closed ledger")
    void shouldPreventRecordingTransactionsOnClosedLedger() {
        // Close the ledger
        ledger.closeLedger("Testing", "SUPERVISOR");

        // Attempt to record a transaction
        IllegalStateException exception = assertThrows(IllegalStateException.class, () -> {
            ledger.recordPayment("PAY_001", "ASSIST_001", new BigDecimal("100.00"), "HUD_ESG", "4.02",
                               PaymentSubtype.RENT_CURRENT, "LANDLORD_001", "Landlord", LocalDate.now(), null, null, "CASE_MANAGER");
        });

        assertTrue(exception.getMessage().contains("Cannot record transactions on closed ledger"));
    }

    @Test
    @DisplayName("Should provide entries for landlord view with VAWA protection")
    void shouldProvideEntriesForLandlordViewWithVawaProtection() {
        // Create VAWA protected ledger
        FinancialLedger vawaLedger = FinancialLedger.create(
            clientId, enrollmentId, householdId, "VAWA Protected Ledger", true, "CASE_MANAGER"
        );

        vawaLedger.recordPayment("PAY_001", "ASSIST_001", new BigDecimal("1500.00"), "HUD_ESG", "4.02",
                               PaymentSubtype.RENT_CURRENT, "LANDLORD_001", "ABC Property", LocalDate.now(), null, null, "CASE_MANAGER");

        // Get landlord view
        var landlordEntries = vawaLedger.getEntriesForLandlordView("LANDLORD_001");

        assertFalse(landlordEntries.isEmpty());

        // Verify VAWA redaction
        for (var entry : landlordEntries) {
            if (entry.getDescription().contains("VAWA PROTECTED")) {
                assertNull(entry.getFundingSourceCode(), "Funding source should be redacted");
                assertEquals("[SYSTEM]", entry.getRecordedBy(), "Recorded by should be redacted");
            }
        }
    }

    @Test
    @DisplayName("Should validate transaction amounts are positive")
    void shouldValidateTransactionAmountsArePositive() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            ledger.recordPayment("PAY_001", "ASSIST_001", BigDecimal.ZERO, "HUD_ESG", "4.02",
                               PaymentSubtype.RENT_CURRENT, "LANDLORD_001", "Landlord", LocalDate.now(), null, null, "CASE_MANAGER");
        });

        assertTrue(exception.getMessage().contains("Amount must be positive"));
    }

    @Test
    @DisplayName("Should validate arrears period dates")
    void shouldValidateArrearsPeriodDates() {
        LocalDate futureDate = LocalDate.now().plusDays(1);
        LocalDate todayDate = LocalDate.now();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            ledger.recordArrears("ARR_001", new BigDecimal("500.00"), ArrearsType.RENT,
                               "LANDLORD_001", "Landlord", futureDate, todayDate, "CASE_MANAGER");
        });

        assertTrue(exception.getMessage().contains("Arrears period cannot be in the future"));
    }
}