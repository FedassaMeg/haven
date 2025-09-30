package org.haven.financialassistance.application.services;

import org.haven.clientprofile.domain.ClientId;
import org.haven.financialassistance.domain.ledger.*;
import org.haven.programenrollment.domain.ProgramEnrollmentId;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

/**
 * Integration tests for FinancialLedgerService
 */
@ExtendWith(MockitoExtension.class)
class FinancialLedgerServiceTest {

    @Mock
    private FinancialLedgerRepository ledgerRepository;

    @Mock
    private VawaLedgerRedactionService redactionService;

    private FinancialLedgerService ledgerService;

    private ClientId clientId;
    private ProgramEnrollmentId enrollmentId;
    private UUID householdId;
    private FinancialLedger testLedger;

    @BeforeEach
    void setUp() {
        ledgerService = new FinancialLedgerService(ledgerRepository, redactionService);

        clientId = new ClientId(UUID.randomUUID());
        enrollmentId = new ProgramEnrollmentId(UUID.randomUUID());
        householdId = UUID.randomUUID();

        testLedger = FinancialLedger.create(
            clientId, enrollmentId, householdId, "Test Ledger", false, "TEST_USER"
        );
    }

    @Test
    @DisplayName("Should create new ledger successfully")
    void shouldCreateNewLedgerSuccessfully() {
        // When
        FinancialLedgerId ledgerId = ledgerService.createLedger(
            clientId, enrollmentId, householdId, "New Ledger", false, "CASE_MANAGER"
        );

        // Then
        assertNotNull(ledgerId);
        verify(ledgerRepository).save(any(FinancialLedger.class));
    }

    @Test
    @DisplayName("Should record payment transaction in existing ledger")
    void shouldRecordPaymentTransactionInExistingLedger() {
        // Given
        when(ledgerRepository.findById(testLedger.getId())).thenReturn(Optional.of(testLedger));

        // When
        ledgerService.recordPaymentTransaction(
            testLedger.getId(),
            "PAY_001",
            "ASSIST_001",
            new BigDecimal("1500.00"),
            "HUD_ESG",
            "4.02",
            PaymentSubtype.RENT_CURRENT,
            "LANDLORD_001",
            "ABC Property",
            LocalDate.now(),
            null,
            null,
            "CASE_MANAGER"
        );

        // Then
        verify(ledgerRepository).findById(testLedger.getId());
        verify(ledgerRepository).save(testLedger);
        assertEquals(2, testLedger.getEntries().size()); // Double-entry
        assertTrue(testLedger.isBalanced());
    }

    @Test
    @DisplayName("Should throw exception when ledger not found")
    void shouldThrowExceptionWhenLedgerNotFound() {
        // Given
        FinancialLedgerId nonExistentId = FinancialLedgerId.generate();
        when(ledgerRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // When & Then
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            ledgerService.recordPaymentTransaction(
                nonExistentId, "PAY_001", "ASSIST_001", new BigDecimal("100.00"),
                "HUD_ESG", "4.02", PaymentSubtype.RENT_CURRENT, "LANDLORD_001", "Landlord",
                LocalDate.now(), null, null, "CASE_MANAGER"
            );
        });

        assertTrue(exception.getMessage().contains("Ledger not found"));
    }

    @Test
    @DisplayName("Should record funding deposit successfully")
    void shouldRecordFundingDepositSuccessfully() {
        // Given
        when(ledgerRepository.findById(testLedger.getId())).thenReturn(Optional.of(testLedger));

        // When
        ledgerService.recordFundingDeposit(
            testLedger.getId(),
            "DEP_001",
            new BigDecimal("10000.00"),
            "HUD_ESG",
            "Emergency Solutions Grant",
            LocalDate.now(),
            "FINANCIAL_ADMIN"
        );

        // Then
        verify(ledgerRepository).save(testLedger);
        assertEquals(2, testLedger.getEntries().size());
        assertTrue(testLedger.isBalanced());
    }

    @Test
    @DisplayName("Should record arrears with validation")
    void shouldRecordArrearsWithValidation() {
        // Given
        when(ledgerRepository.findById(testLedger.getId())).thenReturn(Optional.of(testLedger));

        LocalDate periodStart = LocalDate.now().minusMonths(2);
        LocalDate periodEnd = LocalDate.now().minusMonths(1);

        // When
        ledgerService.recordArrears(
            testLedger.getId(),
            "ARR_001",
            new BigDecimal("800.00"),
            ArrearsType.RENT,
            "LANDLORD_001",
            "ABC Property",
            periodStart,
            periodEnd,
            "CASE_MANAGER"
        );

        // Then
        verify(ledgerRepository).save(testLedger);
        assertEquals(2, testLedger.getEntries().size());

        // Verify arrears entry has period information
        boolean hasArrearsWithPeriod = testLedger.getEntries().stream()
            .anyMatch(entry -> entry.getPeriodStart() != null &&
                             entry.getPeriodEnd() != null &&
                             entry.getDescription().contains("arrears"));

        assertTrue(hasArrearsWithPeriod);
    }

    @Test
    @DisplayName("Should record landlord communication")
    void shouldRecordLandlordCommunication() {
        // Given
        when(ledgerRepository.findById(testLedger.getId())).thenReturn(Optional.of(testLedger));

        // When
        ledgerService.recordLandlordCommunication(
            testLedger.getId(),
            "COMM_001",
            "LANDLORD_001",
            "ABC Property",
            CommunicationType.EMAIL,
            "Payment Notification",
            "Payment has been processed",
            LocalDate.now(),
            "CASE_MANAGER"
        );

        // Then
        verify(ledgerRepository).save(testLedger);
    }

    @Test
    @DisplayName("Should attach document to ledger")
    void shouldAttachDocumentToLedger() {
        // Given
        when(ledgerRepository.findById(testLedger.getId())).thenReturn(Optional.of(testLedger));
        byte[] documentContent = "Sample document".getBytes();

        // When
        ledgerService.attachDocument(
            testLedger.getId(),
            "DOC_001",
            "receipt.pdf",
            "application/pdf",
            "CASE_MANAGER",
            documentContent
        );

        // Then
        verify(ledgerRepository).save(testLedger);
    }

    @Test
    @DisplayName("Should get landlord view with redaction service")
    void shouldGetLandlordViewWithRedactionService() {
        // Given
        when(ledgerRepository.findById(testLedger.getId())).thenReturn(Optional.of(testLedger));

        VawaLedgerRedactionService.LedgerLandlordView mockView =
            new VawaLedgerRedactionService.LedgerLandlordView(
                testLedger.getId().value(),
                testLedger.getClientId().value(),
                "Test Client",
                "LANDLORD_001",
                List.of(),
                BigDecimal.ZERO,
                false
            );

        when(redactionService.createLandlordView(testLedger, "LANDLORD_001")).thenReturn(mockView);

        // When
        Optional<VawaLedgerRedactionService.LedgerLandlordView> result =
            ledgerService.getLandlordView(testLedger.getId(), "LANDLORD_001");

        // Then
        assertTrue(result.isPresent());
        assertEquals("LANDLORD_001", result.get().getLandlordId());
        verify(redactionService).createLandlordView(testLedger, "LANDLORD_001");
    }

    @Test
    @DisplayName("Should get client ledgers")
    void shouldGetClientLedgers() {
        // Given
        List<FinancialLedger> expectedLedgers = List.of(testLedger);
        when(ledgerRepository.findByClientId(clientId)).thenReturn(expectedLedgers);

        // When
        List<FinancialLedger> result = ledgerService.getClientLedgers(clientId);

        // Then
        assertEquals(1, result.size());
        assertEquals(testLedger, result.get(0));
        verify(ledgerRepository).findByClientId(clientId);
    }

    @Test
    @DisplayName("Should get active client ledger")
    void shouldGetActiveClientLedger() {
        // Given
        when(ledgerRepository.findByClientIdAndStatus(clientId, LedgerStatus.ACTIVE))
            .thenReturn(Optional.of(testLedger));

        // When
        Optional<FinancialLedger> result = ledgerService.getActiveClientLedger(clientId);

        // Then
        assertTrue(result.isPresent());
        assertEquals(testLedger, result.get());
        verify(ledgerRepository).findByClientIdAndStatus(clientId, LedgerStatus.ACTIVE);
    }

    @Test
    @DisplayName("Should create ledger when no active ledger exists")
    void shouldCreateLedgerWhenNoActiveLedgerExists() {
        // Given
        when(ledgerRepository.findByClientIdAndStatus(clientId, LedgerStatus.ACTIVE))
            .thenReturn(Optional.empty());
        when(ledgerRepository.findById(any(FinancialLedgerId.class)))
            .thenReturn(Optional.of(testLedger));

        // When
        FinancialLedger result = ledgerService.getOrCreateActiveLedger(
            clientId, enrollmentId, householdId, false, "CASE_MANAGER"
        );

        // Then
        assertNotNull(result);
        verify(ledgerRepository).save(any(FinancialLedger.class));
    }

    @Test
    @DisplayName("Should return existing active ledger when it exists")
    void shouldReturnExistingActiveLedgerWhenItExists() {
        // Given
        when(ledgerRepository.findByClientIdAndStatus(clientId, LedgerStatus.ACTIVE))
            .thenReturn(Optional.of(testLedger));

        // When
        FinancialLedger result = ledgerService.getOrCreateActiveLedger(
            clientId, enrollmentId, householdId, false, "CASE_MANAGER"
        );

        // Then
        assertEquals(testLedger, result);
        verify(ledgerRepository, never()).save(any(FinancialLedger.class));
    }

    @Test
    @DisplayName("Should close ledger successfully")
    void shouldCloseLedgerSuccessfully() {
        // Given
        when(ledgerRepository.findById(testLedger.getId())).thenReturn(Optional.of(testLedger));

        // When
        ledgerService.closeLedger(testLedger.getId(), "End of program", "SUPERVISOR");

        // Then
        assertEquals(LedgerStatus.CLOSED, testLedger.getStatus());
        verify(ledgerRepository).save(testLedger);
    }

    @Test
    @DisplayName("Should find unbalanced ledgers")
    void shouldFindUnbalancedLedgers() {
        // Given
        List<FinancialLedger> unbalancedLedgers = List.of(testLedger);
        when(ledgerRepository.findUnbalancedLedgers()).thenReturn(unbalancedLedgers);

        // When
        List<FinancialLedger> result = ledgerService.findUnbalancedLedgers();

        // Then
        assertEquals(1, result.size());
        verify(ledgerRepository).findUnbalancedLedgers();
    }

    @Test
    @DisplayName("Should find ledgers with overdue arrears")
    void shouldFindLedgersWithOverdueArrears() {
        // Given
        List<FinancialLedger> overdueArrears = List.of(testLedger);
        when(ledgerRepository.findLedgersWithOverdueArrears()).thenReturn(overdueArrears);

        // When
        List<FinancialLedger> result = ledgerService.findLedgersWithOverdueArrears();

        // Then
        assertEquals(1, result.size());
        verify(ledgerRepository).findLedgersWithOverdueArrears();
    }

    @Test
    @DisplayName("Should find ledgers with unmatched deposits")
    void shouldFindLedgersWithUnmatchedDeposits() {
        // Given
        List<FinancialLedger> unmatchedDeposits = List.of(testLedger);
        when(ledgerRepository.findLedgersWithUnmatchedDeposits()).thenReturn(unmatchedDeposits);

        // When
        List<FinancialLedger> result = ledgerService.findLedgersWithUnmatchedDeposits();

        // Then
        assertEquals(1, result.size());
        verify(ledgerRepository).findLedgersWithUnmatchedDeposits();
    }
}