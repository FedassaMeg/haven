package org.haven.api.financialledger;

import org.haven.api.financialledger.dto.*;
import org.haven.clientprofile.domain.ClientId;
import org.haven.financialassistance.application.services.FinancialLedgerService;
import org.haven.financialassistance.application.services.VawaLedgerRedactionService;
import org.haven.financialassistance.domain.ledger.*;
import org.haven.programenrollment.domain.ProgramEnrollmentId;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * REST controller for Financial Ledger operations with comprehensive filtering and role-based access
 */
@RestController
@RequestMapping("/api/financial-ledger")
@CrossOrigin(origins = "*")
public class FinancialLedgerController {

    private final FinancialLedgerService ledgerService;
    private final FinancialLedgerQueryService queryService;

    public FinancialLedgerController(FinancialLedgerService ledgerService,
                                   FinancialLedgerQueryService queryService) {
        this.ledgerService = ledgerService;
        this.queryService = queryService;
    }

    /**
     * Create a new financial ledger
     */
    @PostMapping
    @PreAuthorize("hasRole('CASE_MANAGER') or hasRole('FINANCIAL_ADMIN')")
    public ResponseEntity<LedgerCreateResponse> createLedger(@Valid @RequestBody CreateLedgerRequest request) {
        FinancialLedgerId ledgerId = ledgerService.createLedger(
            new ClientId(request.clientId()),
            new ProgramEnrollmentId(request.enrollmentId()),
            request.householdId(),
            request.ledgerName(),
            request.isVawaProtected(),
            getCurrentUser()
        );

        return ResponseEntity.ok(new LedgerCreateResponse(ledgerId.value()));
    }

    /**
     * Get ledger details with full access (internal users)
     */
    @GetMapping("/{ledgerId}")
    @PreAuthorize("hasRole('CASE_MANAGER') or hasRole('FINANCIAL_ADMIN') or hasRole('SUPERVISOR')")
    public ResponseEntity<LedgerDetailResponse> getLedger(@PathVariable UUID ledgerId) {
        Optional<FinancialLedger> ledger = ledgerService.getClientLedgers(null).stream()
            .filter(l -> l.getId().value().equals(ledgerId))
            .findFirst();

        return ledger.map(l -> ResponseEntity.ok(LedgerDetailResponse.fromDomain(l)))
                    .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Get ledger view for landlords (with VAWA protections)
     */
    @GetMapping("/{ledgerId}/landlord-view")
    @PreAuthorize("hasRole('LANDLORD') or hasRole('VENDOR')")
    public ResponseEntity<LandlordLedgerViewResponse> getLandlordView(@PathVariable UUID ledgerId,
                                                                    @RequestParam String landlordId) {
        Optional<VawaLedgerRedactionService.LedgerLandlordView> view =
            ledgerService.getLandlordView(FinancialLedgerId.of(ledgerId), landlordId);

        return view.map(v -> ResponseEntity.ok(LandlordLedgerViewResponse.fromDomain(v)))
                  .orElse(ResponseEntity.notFound().build());
    }

    /**
     * Search ledgers with comprehensive filtering
     */
    @GetMapping("/search")
    @PreAuthorize("hasRole('CASE_MANAGER') or hasRole('FINANCIAL_ADMIN') or hasRole('SUPERVISOR')")
    public ResponseEntity<Page<LedgerSummaryResponse>> searchLedgers(
            @RequestParam(required = false) UUID clientId,
            @RequestParam(required = false) UUID householdId,
            @RequestParam(required = false) String fundingSourceCode,
            @RequestParam(required = false) LedgerStatus status,
            @RequestParam(required = false) Boolean hasOverdueArrears,
            @RequestParam(required = false) Boolean hasUnmatchedDeposits,
            @RequestParam(required = false) LocalDate createdAfter,
            @RequestParam(required = false) LocalDate createdBefore,
            @RequestParam(required = false) BigDecimal minBalance,
            @RequestParam(required = false) BigDecimal maxBalance,
            Pageable pageable) {

        LedgerSearchCriteria criteria = new LedgerSearchCriteria(
            clientId, householdId, fundingSourceCode, status, hasOverdueArrears,
            hasUnmatchedDeposits, createdAfter, createdBefore, minBalance, maxBalance
        );

        Page<LedgerSummaryResponse> results = queryService.searchLedgers(criteria, pageable);
        return ResponseEntity.ok(results);
    }

    /**
     * Get entries for a specific ledger with filtering
     */
    @GetMapping("/{ledgerId}/entries")
    @PreAuthorize("hasRole('CASE_MANAGER') or hasRole('FINANCIAL_ADMIN') or hasRole('SUPERVISOR')")
    public ResponseEntity<Page<LedgerEntryResponse>> getLedgerEntries(
            @PathVariable UUID ledgerId,
            @RequestParam(required = false) EntryType entryType,
            @RequestParam(required = false) AccountClassification accountClassification,
            @RequestParam(required = false) String payeeId,
            @RequestParam(required = false) String fundingSourceCode,
            @RequestParam(required = false) String hudCategoryCode,
            @RequestParam(required = false) LocalDate fromDate,
            @RequestParam(required = false) LocalDate toDate,
            Pageable pageable) {

        EntrySearchCriteria criteria = new EntrySearchCriteria(
            ledgerId, entryType, accountClassification, payeeId, fundingSourceCode,
            hudCategoryCode, fromDate, toDate
        );

        Page<LedgerEntryResponse> entries = queryService.searchEntries(criteria, pageable);
        return ResponseEntity.ok(entries);
    }

    /**
     * Record a payment transaction
     */
    @PostMapping("/{ledgerId}/transactions/payment")
    @PreAuthorize("hasRole('CASE_MANAGER') or hasRole('FINANCIAL_ADMIN')")
    public ResponseEntity<Void> recordPayment(@PathVariable UUID ledgerId,
                                            @Valid @RequestBody RecordPaymentRequest request) {
        ledgerService.recordPaymentTransaction(
            FinancialLedgerId.of(ledgerId),
            request.paymentId(),
            request.assistanceId(),
            request.amount(),
            request.fundingSourceCode(),
            request.hudCategoryCode(),
            request.subtype(),
            request.payeeId(),
            request.payeeName(),
            request.paymentDate(),
            request.periodStart(),
            request.periodEnd(),
            getCurrentUser()
        );

        return ResponseEntity.ok().build();
    }

    /**
     * Record funding deposit
     */
    @PostMapping("/{ledgerId}/transactions/deposit")
    @PreAuthorize("hasRole('FINANCIAL_ADMIN') or hasRole('SUPERVISOR')")
    public ResponseEntity<Void> recordDeposit(@PathVariable UUID ledgerId,
                                            @Valid @RequestBody RecordDepositRequest request) {
        ledgerService.recordFundingDeposit(
            FinancialLedgerId.of(ledgerId),
            request.depositId(),
            request.amount(),
            request.fundingSourceCode(),
            request.depositSource(),
            request.depositDate(),
            getCurrentUser()
        );

        return ResponseEntity.ok().build();
    }

    /**
     * Record arrears
     */
    @PostMapping("/{ledgerId}/transactions/arrears")
    @PreAuthorize("hasRole('CASE_MANAGER') or hasRole('FINANCIAL_ADMIN')")
    public ResponseEntity<Void> recordArrears(@PathVariable UUID ledgerId,
                                            @Valid @RequestBody RecordArrearsRequest request) {
        ledgerService.recordArrears(
            FinancialLedgerId.of(ledgerId),
            request.arrearsId(),
            request.amount(),
            request.arrearsType(),
            request.payeeId(),
            request.payeeName(),
            request.periodStart(),
            request.periodEnd(),
            getCurrentUser()
        );

        return ResponseEntity.ok().build();
    }

    /**
     * Record landlord communication
     */
    @PostMapping("/{ledgerId}/communications")
    @PreAuthorize("hasRole('CASE_MANAGER') or hasRole('FINANCIAL_ADMIN')")
    public ResponseEntity<Void> recordCommunication(@PathVariable UUID ledgerId,
                                                  @Valid @RequestBody RecordCommunicationRequest request) {
        ledgerService.recordLandlordCommunication(
            FinancialLedgerId.of(ledgerId),
            request.communicationId(),
            request.landlordId(),
            request.landlordName(),
            request.communicationType(),
            request.subject(),
            request.content(),
            request.communicationDate(),
            getCurrentUser()
        );

        return ResponseEntity.ok().build();
    }

    /**
     * Upload document to ledger
     */
    @PostMapping("/{ledgerId}/documents")
    @PreAuthorize("hasRole('CASE_MANAGER') or hasRole('FINANCIAL_ADMIN')")
    public ResponseEntity<Void> uploadDocument(@PathVariable UUID ledgerId,
                                             @Valid @RequestBody UploadDocumentRequest request) {
        ledgerService.attachDocument(
            FinancialLedgerId.of(ledgerId),
            request.documentId(),
            request.documentName(),
            request.documentType(),
            getCurrentUser(),
            request.documentContent()
        );

        return ResponseEntity.ok().build();
    }

    /**
     * Close a ledger
     */
    @PostMapping("/{ledgerId}/close")
    @PreAuthorize("hasRole('FINANCIAL_ADMIN') or hasRole('SUPERVISOR')")
    public ResponseEntity<Void> closeLedger(@PathVariable UUID ledgerId,
                                          @Valid @RequestBody CloseLedgerRequest request) {
        ledgerService.closeLedger(
            FinancialLedgerId.of(ledgerId),
            request.reason(),
            getCurrentUser()
        );

        return ResponseEntity.ok().build();
    }

    /**
     * Get audit reports for unbalanced ledgers
     */
    @GetMapping("/audit/unbalanced")
    @PreAuthorize("hasRole('FINANCIAL_ADMIN') or hasRole('AUDITOR')")
    public ResponseEntity<List<LedgerSummaryResponse>> getUnbalancedLedgers() {
        List<FinancialLedger> unbalanced = ledgerService.findUnbalancedLedgers();
        List<LedgerSummaryResponse> response = unbalanced.stream()
            .map(LedgerSummaryResponse::fromDomain)
            .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * Get ledgers with overdue arrears
     */
    @GetMapping("/alerts/overdue-arrears")
    @PreAuthorize("hasRole('CASE_MANAGER') or hasRole('FINANCIAL_ADMIN') or hasRole('SUPERVISOR')")
    public ResponseEntity<List<LedgerSummaryResponse>> getOverdueArrears() {
        List<FinancialLedger> overdue = ledgerService.findLedgersWithOverdueArrears();
        List<LedgerSummaryResponse> response = overdue.stream()
            .map(LedgerSummaryResponse::fromDomain)
            .toList();
        return ResponseEntity.ok(response);
    }

    /**
     * Get ledgers with unmatched deposits
     */
    @GetMapping("/alerts/unmatched-deposits")
    @PreAuthorize("hasRole('FINANCIAL_ADMIN') or hasRole('AUDITOR')")
    public ResponseEntity<List<LedgerSummaryResponse>> getUnmatchedDeposits() {
        List<FinancialLedger> unmatched = ledgerService.findLedgersWithUnmatchedDeposits();
        List<LedgerSummaryResponse> response = unmatched.stream()
            .map(LedgerSummaryResponse::fromDomain)
            .toList();
        return ResponseEntity.ok(response);
    }

    private String getCurrentUser() {
        // TODO: Integrate with Spring Security to get current user
        return "CURRENT_USER";
    }
}