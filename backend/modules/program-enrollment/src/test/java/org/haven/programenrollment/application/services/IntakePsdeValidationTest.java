package org.haven.programenrollment.application.services;

import org.haven.api.enrollments.dto.IntakePsdeRequest;
import org.haven.programenrollment.application.validation.IntakePsdeValidationService;
import org.haven.shared.vo.hmis.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import java.time.LocalDate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprehensive QA test scenarios for Intake PSDE validation
 * Covers conditional logic, HUD compliance, and VAWA requirements
 */
class IntakePsdeValidationTest {

    private IntakePsdeValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new IntakePsdeValidationService();
    }

    @Nested
    @DisplayName("Domestic Violence Conditional Logic Tests")
    class DomesticViolenceValidationTests {

        @Test
        @DisplayName("Should require DV recency when DV history is YES")
        void shouldRequireDvRecencyWhenDvHistoryIsYes() {
            IntakePsdeRequest request = createValidRequest()
                .withDomesticViolence(DomesticViolence.YES)
                .withDomesticViolenceRecency(null)
                .build();

            var result = validationService.validateIntakePsdeRequest(request);

            assertThat(result.hasErrors()).isTrue();
            assertThat(result.errors()).anyMatch(error ->
                error.field().equals("domesticViolenceRecency") &&
                error.message().contains("required when DV history is 'Yes'"));
        }

        @Test
        @DisplayName("Should not allow DV recency when DV history is NO")
        void shouldNotAllowDvRecencyWhenDvHistoryIsNo() {
            IntakePsdeRequest request = createValidRequest()
                .withDomesticViolence(DomesticViolence.NO)
                .withDomesticViolenceRecency(DomesticViolenceRecency.WITHIN_3_MONTHS)
                .build();

            var result = validationService.validateIntakePsdeRequest(request);

            assertThat(result.hasErrors()).isTrue();
            assertThat(result.errors()).anyMatch(error ->
                error.field().equals("domesticViolenceRecency") &&
                error.message().contains("should not be collected when DV history is 'No'"));
        }

        @Test
        @DisplayName("Should pass validation for complete DV data")
        void shouldPassValidationForCompleteDvData() {
            IntakePsdeRequest request = createValidRequest()
                .withDomesticViolence(DomesticViolence.YES)
                .withDomesticViolenceRecency(DomesticViolenceRecency.WITHIN_3_MONTHS)
                .withCurrentlyFleeingDomesticViolence(HmisFivePoint.YES)
                .build();

            var result = validationService.validateIntakePsdeRequest(request);

            assertThat(result.isValid()).isTrue();
        }

        @Test
        @DisplayName("Should handle client refused/doesn't know appropriately")
        void shouldHandleClientRefusedAppropriately() {
            IntakePsdeRequest request = createValidRequest()
                .withDomesticViolence(DomesticViolence.CLIENT_REFUSED)
                .withDomesticViolenceRecency(DomesticViolenceRecency.WITHIN_3_MONTHS)
                .build();

            var result = validationService.validateIntakePsdeRequest(request);

            assertThat(result.hasErrors()).isTrue();
            assertThat(result.errors()).anyMatch(error ->
                error.message().contains("should not be collected when DV history is unknown/refused"));
        }
    }

    @Nested
    @DisplayName("Income Conditional Logic Tests")
    class IncomeValidationTests {

        @Test
        @DisplayName("Should require income amount > 0 when income from any source is YES")
        void shouldRequirePositiveIncomeWhenIncomeFromAnySourceIsYes() {
            IntakePsdeRequest request = createValidRequest()
                .withIncomeFromAnySource(IncomeFromAnySource.YES)
                .withTotalMonthlyIncome(0)
                .build();

            var result = validationService.validateIntakePsdeRequest(request);

            assertThat(result.hasErrors()).isTrue();
            assertThat(result.errors()).anyMatch(error ->
                error.field().equals("totalMonthlyIncome") &&
                error.message().contains("should be greater than 0"));
        }

        @Test
        @DisplayName("Should require income amount = 0 when income from any source is NO")
        void shouldRequireZeroIncomeWhenIncomeFromAnySourceIsNo() {
            IntakePsdeRequest request = createValidRequest()
                .withIncomeFromAnySource(IncomeFromAnySource.NO)
                .withTotalMonthlyIncome(1500)
                .build();

            var result = validationService.validateIntakePsdeRequest(request);

            assertThat(result.hasErrors()).isTrue();
            assertThat(result.errors()).anyMatch(error ->
                error.field().equals("totalMonthlyIncome") &&
                error.message().contains("should be 0 when 'Income from any source' is 'No'"));
        }
    }

    @Nested
    @DisplayName("RRH Move-in Conditional Logic Tests")
    class RrhMoveInValidationTests {

        @Test
        @DisplayName("Should require move-in type when move-in date is provided")
        void shouldRequireMoveInTypeWhenDateProvided() {
            IntakePsdeRequest request = createValidRequest()
                .withResidentialMoveInDate(LocalDate.now())
                .withMoveInType(null)
                .build();

            var result = validationService.validateIntakePsdeRequest(request);

            assertThat(result.hasErrors()).isTrue();
            assertThat(result.errors()).anyMatch(error ->
                error.field().equals("moveInType") &&
                error.message().contains("required when move-in date is provided"));
        }

        @Test
        @DisplayName("Should require move-in date when move-in type is provided")
        void shouldRequireMoveInDateWhenTypeProvided() {
            IntakePsdeRequest request = createValidRequest()
                .withResidentialMoveInDate(null)
                .withMoveInType(ResidentialMoveInDateType.INITIAL_MOVE_IN)
                .build();

            var result = validationService.validateIntakePsdeRequest(request);

            assertThat(result.hasErrors()).isTrue();
            assertThat(result.errors()).anyMatch(error ->
                error.field().equals("residentialMoveInDate") &&
                error.message().contains("required when move-in type is provided"));
        }
    }

    @Nested
    @DisplayName("HMIS Data Quality Tests")
    class HmisDataQualityTests {

        @Test
        @DisplayName("Should enforce data quality requirements for comprehensive assessment")
        void shouldEnforceDataQualityForComprehensiveAssessment() {
            IntakePsdeRequest request = createBasicRequest()
                .withCollectionStage(IntakeDataCollectionStage.COMPREHENSIVE_ASSESSMENT)
                .withPhysicalDisability(null) // Missing required field
                .build();

            var result = validationService.validateIntakePsdeRequest(request);

            assertThat(result.hasErrors()).isTrue();
            assertThat(result.errors()).anyMatch(error ->
                error.field().equals("physicalDisability") &&
                error.message().contains("must be a known response for HMIS data quality"));
        }

        @Test
        @DisplayName("Should not enforce strict data quality for initial intake")
        void shouldNotEnforceStrictDataQualityForInitialIntake() {
            IntakePsdeRequest request = createBasicRequest()
                .withCollectionStage(IntakeDataCollectionStage.INITIAL_INTAKE)
                .withPhysicalDisability(null) // Would be required for comprehensive assessment
                .build();

            var result = validationService.validateIntakePsdeRequest(request);

            // Should not have data quality errors for initial intake
            assertThat(result.errors()).noneMatch(error ->
                error.message().contains("HMIS data quality"));
        }
    }

    @Nested
    @DisplayName("VAWA Compliance Tests")
    class VawaComplianceTests {

        @Test
        @DisplayName("Should recommend redaction when VAWA confidentiality requested")
        void shouldRecommendRedactionWhenVawaConfidentialityRequested() {
            IntakePsdeRequest request = createValidRequest()
                .withVawaConfidentialityRequested(true)
                .withDvRedactionLevel(DvRedactionFlag.NO_REDACTION)
                .build();

            var result = validationService.validateIntakePsdeRequest(request);

            assertThat(result.hasErrors()).isTrue();
            assertThat(result.errors()).anyMatch(error ->
                error.field().equals("dvRedactionLevel") &&
                error.message().contains("redaction level required when VAWA confidentiality is requested"));
        }

        @Test
        @DisplayName("Should detect high-risk DV cases")
        void shouldDetectHighRiskDvCases() {
            IntakePsdeRequest request = createValidRequest()
                .withDomesticViolence(DomesticViolence.YES)
                .withCurrentlyFleeingDomesticViolence(HmisFivePoint.YES)
                .withDomesticViolenceRecency(DomesticViolenceRecency.WITHIN_3_MONTHS)
                .build();

            // High-risk case should be valid but should trigger recommendations
            var result = validationService.validateIntakePsdeRequest(request);
            assertThat(result.isValid()).isTrue();

            // Check that request is identified as high-risk
            assertThat(request.containsSensitiveInformation()).isTrue();
            assertThat(request.getRecommendedRedactionLevel())
                .isEqualTo(DvRedactionFlag.REDACT_FOR_NON_DV_SPECIALISTS);
        }
    }

    @Nested
    @DisplayName("Role-Based Redaction Tests")
    class RoleBasedRedactionTests {

        @Test
        @DisplayName("Should allow different access levels based on redaction flags")
        void shouldAllowDifferentAccessLevelsBasedOnRedactionFlags() {
            // Test various redaction levels
            assertThat(DvRedactionFlag.NO_REDACTION.blocksAccessForRole("CASE_MANAGER")).isFalse();
            assertThat(DvRedactionFlag.REDACT_FOR_GENERAL_STAFF.blocksAccessForRole("CASE_MANAGER")).isTrue();
            assertThat(DvRedactionFlag.REDACT_FOR_GENERAL_STAFF.blocksAccessForRole("DV_SPECIALIST")).isFalse();
            assertThat(DvRedactionFlag.REDACT_FOR_NON_DV_SPECIALISTS.blocksAccessForRole("DV_SPECIALIST")).isFalse();
            assertThat(DvRedactionFlag.VICTIM_REQUESTED_CONFIDENTIALITY.blocksAccessForRole("DV_SPECIALIST")).isTrue();
        }
    }

    // Helper methods for creating test data
    private IntakePsdeRequestBuilder createValidRequest() {
        return new IntakePsdeRequestBuilder()
            .withInformationDate(LocalDate.now())
            .withCollectionStage(IntakeDataCollectionStage.COMPREHENSIVE_ASSESSMENT)
            .withIncomeFromAnySource(IncomeFromAnySource.NO)
            .withTotalMonthlyIncome(0)
            .withCoveredByHealthInsurance(CoveredByHealthInsurance.YES)
            .withPhysicalDisability(DisabilityType.NO)
            .withDevelopmentalDisability(DisabilityType.NO)
            .withChronicHealthCondition(DisabilityType.NO)
            .withHivAids(DisabilityType.NO)
            .withMentalHealthDisorder(DisabilityType.NO)
            .withSubstanceUseDisorder(DisabilityType.NO)
            .withDomesticViolence(DomesticViolence.NO)
            .withCollectedBy("test-user");
    }

    private IntakePsdeRequestBuilder createBasicRequest() {
        return new IntakePsdeRequestBuilder()
            .withInformationDate(LocalDate.now())
            .withCollectionStage(IntakeDataCollectionStage.INITIAL_INTAKE)
            .withCollectedBy("test-user");
    }

    // Builder pattern for test data creation
    private static class IntakePsdeRequestBuilder {
        private LocalDate informationDate;
        private IntakeDataCollectionStage collectionStage;
        private Integer totalMonthlyIncome;
        private IncomeFromAnySource incomeFromAnySource;
        private CoveredByHealthInsurance coveredByHealthInsurance;
        private DisabilityType physicalDisability;
        private DisabilityType developmentalDisability;
        private DisabilityType chronicHealthCondition;
        private DisabilityType hivAids;
        private DisabilityType mentalHealthDisorder;
        private DisabilityType substanceUseDisorder;
        private DomesticViolence domesticViolence;
        private DomesticViolenceRecency domesticViolenceRecency;
        private HmisFivePoint currentlyFleeingDomesticViolence;
        private DvRedactionFlag dvRedactionLevel;
        private Boolean vawaConfidentialityRequested;
        private LocalDate residentialMoveInDate;
        private ResidentialMoveInDateType moveInType;
        private String collectedBy;

        public IntakePsdeRequestBuilder withInformationDate(LocalDate date) {
            this.informationDate = date;
            return this;
        }

        public IntakePsdeRequestBuilder withCollectionStage(IntakeDataCollectionStage stage) {
            this.collectionStage = stage;
            return this;
        }

        public IntakePsdeRequestBuilder withTotalMonthlyIncome(Integer income) {
            this.totalMonthlyIncome = income;
            return this;
        }

        public IntakePsdeRequestBuilder withIncomeFromAnySource(IncomeFromAnySource income) {
            this.incomeFromAnySource = income;
            return this;
        }

        public IntakePsdeRequestBuilder withCoveredByHealthInsurance(CoveredByHealthInsurance covered) {
            this.coveredByHealthInsurance = covered;
            return this;
        }

        public IntakePsdeRequestBuilder withPhysicalDisability(DisabilityType disability) {
            this.physicalDisability = disability;
            return this;
        }

        public IntakePsdeRequestBuilder withDevelopmentalDisability(DisabilityType disability) {
            this.developmentalDisability = disability;
            return this;
        }

        public IntakePsdeRequestBuilder withChronicHealthCondition(DisabilityType condition) {
            this.chronicHealthCondition = condition;
            return this;
        }

        public IntakePsdeRequestBuilder withHivAids(DisabilityType hivAids) {
            this.hivAids = hivAids;
            return this;
        }

        public IntakePsdeRequestBuilder withMentalHealthDisorder(DisabilityType disorder) {
            this.mentalHealthDisorder = disorder;
            return this;
        }

        public IntakePsdeRequestBuilder withSubstanceUseDisorder(DisabilityType disorder) {
            this.substanceUseDisorder = disorder;
            return this;
        }

        public IntakePsdeRequestBuilder withDomesticViolence(DomesticViolence dv) {
            this.domesticViolence = dv;
            return this;
        }

        public IntakePsdeRequestBuilder withDomesticViolenceRecency(DomesticViolenceRecency recency) {
            this.domesticViolenceRecency = recency;
            return this;
        }

        public IntakePsdeRequestBuilder withCurrentlyFleeingDomesticViolence(HmisFivePoint fleeing) {
            this.currentlyFleeingDomesticViolence = fleeing;
            return this;
        }

        public IntakePsdeRequestBuilder withDvRedactionLevel(DvRedactionFlag level) {
            this.dvRedactionLevel = level;
            return this;
        }

        public IntakePsdeRequestBuilder withVawaConfidentialityRequested(Boolean requested) {
            this.vawaConfidentialityRequested = requested;
            return this;
        }

        public IntakePsdeRequestBuilder withResidentialMoveInDate(LocalDate date) {
            this.residentialMoveInDate = date;
            return this;
        }

        public IntakePsdeRequestBuilder withMoveInType(ResidentialMoveInDateType type) {
            this.moveInType = type;
            return this;
        }

        public IntakePsdeRequestBuilder withCollectedBy(String user) {
            this.collectedBy = user;
            return this;
        }

        public IntakePsdeRequest build() {
            return new IntakePsdeRequest(
                informationDate, collectionStage, totalMonthlyIncome, incomeFromAnySource,
                null, null, coveredByHealthInsurance, null, null,
                physicalDisability, developmentalDisability, chronicHealthCondition,
                hivAids, mentalHealthDisorder, substanceUseDisorder, null,
                domesticViolence, domesticViolenceRecency, currentlyFleeingDomesticViolence,
                dvRedactionLevel, vawaConfidentialityRequested,
                residentialMoveInDate, moveInType, null, collectedBy
            );
        }
    }
}