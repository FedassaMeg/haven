package org.haven.housingassistance.domain;

import org.haven.clientprofile.domain.ClientId;
import org.haven.programenrollment.domain.ProgramEnrollmentId;
import org.haven.housingassistance.domain.HousingAssistance.AssistancePaymentSubtype;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;

/**
 * Unit tests for arrears payment validation in HousingAssistance domain
 */
class HousingAssistanceArrearsTest {
    
    private HousingAssistance housingAssistance;
    private ClientId clientId;
    private ProgramEnrollmentId enrollmentId;
    
    @BeforeEach
    void setUp() {
        clientId = new ClientId(UUID.randomUUID());
        enrollmentId = new ProgramEnrollmentId(UUID.randomUUID());
        
        // Create and approve housing assistance
        housingAssistance = HousingAssistance.request(
            clientId,
            enrollmentId,
            RentalAssistanceType.RRH_MEDIUM_TERM_RENTAL_ASSISTANCE,
            new BigDecimal("10000"),
            12,
            "Family needs housing assistance",
            "case-manager-123"
        );
        
        housingAssistance.initiateApproval("SUPERVISOR", 1);
        housingAssistance.approveAssistance(
            new BigDecimal("10000"),
            12,
            "FEDERAL_GRANT_001",
            "supervisor-456",
            "Approved for 12 months"
        );
        
        housingAssistance.assignUnit(
            "unit-001",
            "landlord-001",
            new BigDecimal("1500"),
            LocalDate.now(),
            LocalDate.now().plusMonths(12)
        );
    }
    
    @Test
    @DisplayName("Should successfully authorize rent arrears payment with valid period")
    void authorizePayment_RentArrears_WithValidPeriod_Success() {
        // Given
        LocalDate periodStart = LocalDate.now().minusMonths(3);
        LocalDate periodEnd = LocalDate.now().minusMonths(1);
        BigDecimal amount = new BigDecimal("4500"); // 3 months * $1500
        
        // When
        housingAssistance.authorizePayment(
            amount,
            LocalDate.now(),
            "RENT",
            AssistancePaymentSubtype.RENT_ARREARS,
            periodStart,
            periodEnd,
            "landlord-001",
            "Test Landlord",
            "case-manager-123"
        );
        
        // Then
        assertThat(housingAssistance.getPayments()).hasSize(1);
        var payment = housingAssistance.getPayments().get(0);
        assertThat(payment.getSubtype()).isEqualTo(AssistancePaymentSubtype.RENT_ARREARS);
        assertThat(payment.getPeriodStart()).isEqualTo(periodStart);
        assertThat(payment.getPeriodEnd()).isEqualTo(periodEnd);
        assertThat(payment.getAmount()).isEqualTo(amount);
    }
    
    @Test
    @DisplayName("Should successfully authorize utility arrears payment with valid period")
    void authorizePayment_UtilityArrears_WithValidPeriod_Success() {
        // Given
        LocalDate periodStart = LocalDate.now().minusMonths(2);
        LocalDate periodEnd = LocalDate.now().minusMonths(1);
        BigDecimal amount = new BigDecimal("300"); // 2 months utilities
        
        // When
        housingAssistance.authorizePayment(
            amount,
            LocalDate.now(),
            "UTILITIES",
            AssistancePaymentSubtype.UTILITY_ARREARS,
            periodStart,
            periodEnd,
            "utility-company-001",
            "City Power & Light",
            "case-manager-123"
        );
        
        // Then
        assertThat(housingAssistance.getPayments()).hasSize(1);
        var payment = housingAssistance.getPayments().get(0);
        assertThat(payment.getSubtype()).isEqualTo(AssistancePaymentSubtype.UTILITY_ARREARS);
        assertThat(payment.getPeriodStart()).isEqualTo(periodStart);
        assertThat(payment.getPeriodEnd()).isEqualTo(periodEnd);
    }
    
    @Test
    @DisplayName("Should reject arrears payment without period dates")
    void authorizePayment_Arrears_WithoutPeriod_ThrowsException() {
        // Given
        BigDecimal amount = new BigDecimal("3000");
        
        // When/Then
        assertThatThrownBy(() -> 
            housingAssistance.authorizePayment(
                amount,
                LocalDate.now(),
                "RENT",
                AssistancePaymentSubtype.RENT_ARREARS,
                null, // Missing period start
                null, // Missing period end
                "landlord-001",
                "Test Landlord",
                "case-manager-123"
            )
        )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Arrears payments must specify period start and end dates");
    }
    
    @Test
    @DisplayName("Should reject arrears payment with future period")
    void authorizePayment_Arrears_WithFuturePeriod_ThrowsException() {
        // Given
        LocalDate periodStart = LocalDate.now().plusMonths(1); // Future date
        LocalDate periodEnd = LocalDate.now().plusMonths(2);
        BigDecimal amount = new BigDecimal("3000");
        
        // When/Then
        assertThatThrownBy(() -> 
            housingAssistance.authorizePayment(
                amount,
                LocalDate.now(),
                "RENT",
                AssistancePaymentSubtype.RENT_ARREARS,
                periodStart,
                periodEnd,
                "landlord-001",
                "Test Landlord",
                "case-manager-123"
            )
        )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Arrears period cannot be in the future");
    }
    
    @Test
    @DisplayName("Should reject arrears payment exceeding approved duration")
    void authorizePayment_Arrears_ExceedingDuration_ThrowsException() {
        // Given - 13 months should exceed the 12-month approved duration
        LocalDate periodStart = LocalDate.now().minusMonths(13); // 13 months back
        LocalDate periodEnd = LocalDate.now().minusMonths(1);   // 1 month back (so 12 months span)
        BigDecimal amount = new BigDecimal("1000"); // Small amount to avoid budget check
        
        // When/Then
        assertThatThrownBy(() -> 
            housingAssistance.authorizePayment(
                amount,
                LocalDate.now(),
                "RENT",
                AssistancePaymentSubtype.RENT_ARREARS,
                periodStart,
                periodEnd,
                "landlord-001",
                "Test Landlord",
                "case-manager-123"
            )
        )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Arrears period exceeds approved duration");
    }
    
    @Test
    @DisplayName("Should successfully authorize current rent payment without period")
    void authorizePayment_CurrentRent_WithoutPeriod_Success() {
        // Given
        BigDecimal amount = new BigDecimal("1500");
        
        // When
        housingAssistance.authorizePayment(
            amount,
            LocalDate.now(),
            "RENT",
            AssistancePaymentSubtype.RENT_CURRENT,
            null, // No period needed for current rent
            null,
            "landlord-001",
            "Test Landlord",
            "case-manager-123"
        );
        
        // Then
        assertThat(housingAssistance.getPayments()).hasSize(1);
        var payment = housingAssistance.getPayments().get(0);
        assertThat(payment.getSubtype()).isEqualTo(AssistancePaymentSubtype.RENT_CURRENT);
        assertThat(payment.getPeriodStart()).isNull();
        assertThat(payment.getPeriodEnd()).isNull();
    }
    
    @Test
    @DisplayName("Should successfully authorize security deposit payment")
    void authorizePayment_SecurityDeposit_Success() {
        // Given
        BigDecimal amount = new BigDecimal("3000"); // 2 months deposit
        
        // When
        housingAssistance.authorizePayment(
            amount,
            LocalDate.now(),
            "DEPOSIT",
            AssistancePaymentSubtype.SECURITY_DEPOSIT,
            null,
            null,
            "landlord-001",
            "Test Landlord",
            "case-manager-123"
        );
        
        // Then
        assertThat(housingAssistance.getPayments()).hasSize(1);
        var payment = housingAssistance.getPayments().get(0);
        assertThat(payment.getSubtype()).isEqualTo(AssistancePaymentSubtype.SECURITY_DEPOSIT);
    }
    
    @Test
    @DisplayName("Should track multiple arrears payments correctly")
    void authorizePayment_MultipleArrearsPayments_TrackedCorrectly() {
        // Given - First arrears payment
        LocalDate period1Start = LocalDate.now().minusMonths(3);
        LocalDate period1End = LocalDate.now().minusMonths(2);
        BigDecimal amount1 = new BigDecimal("1500");
        
        housingAssistance.authorizePayment(
            amount1,
            LocalDate.now(),
            "RENT",
            AssistancePaymentSubtype.RENT_ARREARS,
            period1Start,
            period1End,
            "landlord-001",
            "Test Landlord",
            "case-manager-123"
        );
        
        // Second arrears payment
        LocalDate period2Start = LocalDate.now().minusMonths(2);
        LocalDate period2End = LocalDate.now().minusMonths(1);
        BigDecimal amount2 = new BigDecimal("1500");
        
        housingAssistance.authorizePayment(
            amount2,
            LocalDate.now(),
            "RENT",
            AssistancePaymentSubtype.RENT_ARREARS,
            period2Start,
            period2End,
            "landlord-001",
            "Test Landlord",
            "case-manager-123"
        );
        
        // Current rent payment
        BigDecimal currentAmount = new BigDecimal("1500");
        housingAssistance.authorizePayment(
            currentAmount,
            LocalDate.now(),
            "RENT",
            AssistancePaymentSubtype.RENT_CURRENT,
            null,
            null,
            "landlord-001",
            "Test Landlord",
            "case-manager-123"
        );
        
        // Then
        assertThat(housingAssistance.getPayments()).hasSize(3);
        
        long arrearsCount = housingAssistance.getPayments().stream()
            .filter(p -> p.getSubtype() == AssistancePaymentSubtype.RENT_ARREARS)
            .count();
        assertThat(arrearsCount).isEqualTo(2);
        
        long currentCount = housingAssistance.getPayments().stream()
            .filter(p -> p.getSubtype() == AssistancePaymentSubtype.RENT_CURRENT)
            .count();
        assertThat(currentCount).isEqualTo(1);
        
        assertThat(housingAssistance.getTotalPaid()).isEqualTo(new BigDecimal("4500"));
    }
    
    @Test
    @DisplayName("Should validate period dates are in correct order")
    void authorizePayment_Arrears_WithInvalidPeriodOrder_ThrowsException() {
        // Given
        LocalDate periodStart = LocalDate.now().minusMonths(1);
        LocalDate periodEnd = LocalDate.now().minusMonths(2); // End before start
        BigDecimal amount = new BigDecimal("1500");
        
        // When/Then
        assertThatThrownBy(() -> 
            housingAssistance.authorizePayment(
                amount,
                LocalDate.now(),
                "RENT",
                AssistancePaymentSubtype.RENT_ARREARS,
                periodStart,
                periodEnd,
                "landlord-001",
                "Test Landlord",
                "case-manager-123"
            )
        )
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessage("Period start date must not be after period end date");
    }
    
    @Test
    @DisplayName("Should calculate arrears period correctly for Payment constructor")
    void payment_ArrearsConstructor_ValidatesCorrectly() {
        // Given
        UUID paymentId = UUID.randomUUID();
        LocalDate periodStart = LocalDate.now().minusMonths(2);
        LocalDate periodEnd = LocalDate.now().minusMonths(1);
        
        // When
        HousingAssistance.Payment payment = new HousingAssistance.Payment(
            paymentId,
            new BigDecimal("3000"),
            LocalDate.now(),
            "RENT",
            AssistancePaymentSubtype.RENT_ARREARS,
            periodStart,
            periodEnd,
            "payee-001",
            "Test Payee",
            "authorizer-001"
        );
        
        // Then
        assertThat(payment.getSubtype()).isEqualTo(AssistancePaymentSubtype.RENT_ARREARS);
        assertThat(payment.getPeriodStart()).isEqualTo(periodStart);
        assertThat(payment.getPeriodEnd()).isEqualTo(periodEnd);
    }
}