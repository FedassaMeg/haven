package org.haven.programenrollment.domain;

import org.haven.clientprofile.domain.ClientId;
import org.haven.shared.domain.AggregateRoot;
import org.haven.shared.events.DomainEvent;
import org.haven.shared.vo.CodeableConcept;
import org.haven.shared.vo.Period;
import org.haven.shared.vo.hmis.*;
import org.haven.programenrollment.domain.events.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * HMIS-aligned Program Enrollment aggregate root
 * Manages the complete enrollment lifecycle: enrollment → services → exit
 */
public class ProgramEnrollment extends AggregateRoot<ProgramEnrollmentId> {
    
    private ClientId clientId;
    private UUID programId;
    private LocalDate enrollmentDate;
    private Period enrollmentPeriod;
    private EnrollmentStatus status;
    
    // HMIS Universal Data Elements for enrollment
    private CodeableConcept relationshipToHead;
    private CodeableConcept residencePriorToEntry;
    private CodeableConcept lengthOfStay;
    private String entryFrom;
    
    // HMIS 2024 Comparable Database fields
    private String householdId;
    private RelationshipToHeadOfHousehold hmisRelationshipToHoH;
    private PriorLivingSituation hmisPriorLivingSituation;
    private LengthOfStay hmisLengthOfStay;
    private DisablingCondition hmisDisablingCondition;
    
    // Joint TH/RRH support
    private UUID predecessorEnrollmentId;
    private LocalDate residentialMoveInDate;
    private HmisProjectType projectType;
    
    // Program Specific Data Elements (HMIS FY2024)
    private ProgramSpecificDataElements programSpecificData;
    
    // Income and Benefits Records (HMIS compliant lifecycle management)
    private List<IncomeBenefitsRecord> incomeBenefitsRecords = new ArrayList<>();
    
    // Physical Disability Records (HMIS UDE 3.08 lifecycle management)
    private List<PhysicalDisabilityRecord> physicalDisabilityRecords = new ArrayList<>();
    
    // All Other Disability Records (HMIS UDE 3.09-3.13 lifecycle management)
    private List<DisabilityRecord> disabilityRecords = new ArrayList<>();
    
    // Domestic Violence Records (HMIS 4.11 lifecycle management)
    private List<DvRecord> dvRecords = new ArrayList<>();
    
    // Current Living Situation Contacts
    private List<CurrentLivingSituation> currentLivingSituations = new ArrayList<>();
    
    // Date of Engagement (single effective record)
    private DateOfEngagement dateOfEngagement;
    
    // Bed Nights (for ES-NbN projects)
    private List<BedNight> bedNights = new ArrayList<>();
    
    // Service episodes during enrollment
    private List<ServiceEpisode> serviceEpisodes = new ArrayList<>();
    
    // Exit information (nullable until exit occurs)
    private ProjectExit projectExit;
    
    private Instant createdAt;
    
    public static ProgramEnrollment create(ClientId clientId, UUID programId, 
                                         LocalDate enrollmentDate, 
                                         CodeableConcept relationshipToHead,
                                         CodeableConcept residencePriorToEntry,
                                         String entryFrom) {
        ProgramEnrollmentId enrollmentId = ProgramEnrollmentId.generate();
        ProgramEnrollment enrollment = new ProgramEnrollment();
        enrollment.apply(new ProgramEnrollmentCreated(
            enrollmentId.value(),
            clientId.value(),
            programId,
            enrollmentDate,
            relationshipToHead,
            residencePriorToEntry,
            null, // lengthOfStay calculated separately
            entryFrom,
            Instant.now()
        ));
        
        // Initialize Program Specific Data Elements
        enrollment.programSpecificData = new ProgramSpecificDataElements();
        
        return enrollment;
    }
    
    /**
     * Set the project type for this enrollment
     * This should be called after creation to ensure proper validation
     */
    public void setProjectType(HmisProjectType projectType) {
        this.projectType = projectType;
    }
    
    public void addServiceEpisode(CodeableConcept serviceType, LocalDate serviceDate, 
                                String providedBy, String description) {
        if (status == EnrollmentStatus.EXITED) {
            throw new IllegalStateException("Cannot add services to exited enrollment");
        }
        
        ServiceEpisodeId episodeId = ServiceEpisodeId.generate();
        apply(new ServiceEpisodeAdded(
            id.value(),
            episodeId.value(),
            serviceType,
            serviceDate,
            providedBy,
            description,
            Instant.now()
        ));
    }
    
    /**
     * Link an external ServiceEpisode to this enrollment
     * Used when ServiceEpisode aggregate is created separately
     */
    public void linkServiceEpisode(ServiceEpisodeId episodeId,
                                 org.haven.shared.vo.services.ServiceType serviceType,
                                 LocalDate serviceDate,
                                 String providedBy) {
        if (status == EnrollmentStatus.EXITED) {
            throw new IllegalStateException("Cannot link services to exited enrollment");
        }
        
        apply(new ServiceEpisodeLinked(
            id.value(),
            episodeId.value(),
            serviceType.name(),
            serviceDate,
            providedBy,
            Instant.now()
        ));
    }
    
    /**
     * Get total service hours for this enrollment
     */
    public Double getTotalServiceHours() {
        // This would typically be calculated from linked ServiceEpisodes
        // For now, return estimated based on episode count
        return serviceEpisodes.size() * 0.75; // Assuming 45 minutes average
    }
    
    /**
     * Get service intensity (services per week)
     */
    public Double getServiceIntensity() {
        if (enrollmentPeriod == null || serviceEpisodes.isEmpty()) {
            return 0.0;
        }
        
        long enrollmentDays = java.time.Duration.between(
            enrollmentPeriod.start(), 
            enrollmentPeriod.end() != null ? enrollmentPeriod.end() : Instant.now()
        ).toDays();
        
        if (enrollmentDays == 0) return 0.0;
        
        double enrollmentWeeks = enrollmentDays / 7.0;
        return serviceEpisodes.size() / enrollmentWeeks;
    }
    
    /**
     * Check if enrollment meets minimum service requirements
     */
    public boolean meetsMinimumServiceRequirements() {
        // This would vary by program type and funder requirements
        // Example: at least 1 service per month for active enrollment
        if (serviceEpisodes.isEmpty()) return false;
        
        // Check if there's been a service in the last 30 days
        Instant thirtyDaysAgo = Instant.now().minusSeconds(30 * 24 * 60 * 60);
        return serviceEpisodes.stream()
            .anyMatch(episode -> episode.getRecordedAt().isAfter(thirtyDaysAgo));
    }
    
    public void exitProgram(LocalDate exitDate, CodeableConcept exitReason, 
                          CodeableConcept destination, String recordedBy) {
        if (status == EnrollmentStatus.EXITED) {
            throw new IllegalStateException("Enrollment is already exited");
        }
        
        apply(new ProgramExited(
            id.value(),
            exitDate,
            exitReason,
            destination,
            null, // housing outcome determined separately
            recordedBy,
            Instant.now()
        ));
    }
    
    @Override
    protected void when(DomainEvent event) {
        if (event instanceof ProgramEnrollmentCreated e) {
            this.id = ProgramEnrollmentId.of(e.enrollmentId());
            this.clientId = new ClientId(e.clientId());
            this.programId = e.programId();
            this.enrollmentDate = e.enrollmentDate();
            this.relationshipToHead = e.relationshipToHead();
            this.residencePriorToEntry = e.residencePriorToEntry();
            this.lengthOfStay = e.lengthOfStay();
            this.entryFrom = e.entryFrom();
            this.status = EnrollmentStatus.ACTIVE;
            this.enrollmentPeriod = new Period(e.occurredAt(), null);
            this.createdAt = e.occurredAt();
            
        } else if (event instanceof ServiceEpisodeAdded e) {
            ServiceEpisode episode = new ServiceEpisode(
                ServiceEpisodeId.of(e.serviceEpisodeId()),
                e.serviceType(),
                e.serviceDate(),
                e.providedBy()
            );
            this.serviceEpisodes.add(episode);
            
        } else if (event instanceof ProgramExited e) {
            this.projectExit = new ProjectExit(
                e.exitDate(),
                e.exitReason(),
                e.destination(),
                e.recordedBy()
            );
            this.status = EnrollmentStatus.EXITED;
            if (this.enrollmentPeriod != null) {
                this.enrollmentPeriod = new Period(
                    this.enrollmentPeriod.start(), 
                    e.occurredAt()
                );
            }
            
        } else if (event instanceof EnrollmentTransitionedToRrh e) {
            // This event is primarily for external systems (like creating the RRH enrollment)
            // The TH enrollment itself doesn't change state during transition
            // The event signals that a new RRH enrollment should be created
            
        } else if (event instanceof ResidentialMoveInDateUpdated e) {
            this.residentialMoveInDate = e.moveInDate();
            
        } else {
            throw new IllegalArgumentException("Unhandled event: " + event.getClass());
        }
    }
    
    public enum EnrollmentStatus {
        PENDING,
        ACTIVE,
        SUSPENDED,
        EXITED,
        CANCELLED
    }
    
    // Getters
    public ClientId getClientId() { return clientId; }
    public UUID getProgramId() { return programId; }
    public LocalDate getEnrollmentDate() { return enrollmentDate; }
    public Period getEnrollmentPeriod() { return enrollmentPeriod; }
    public EnrollmentStatus getStatus() { return status; }
    public CodeableConcept getRelationshipToHead() { return relationshipToHead; }
    public CodeableConcept getResidencePriorToEntry() { return residencePriorToEntry; }
    public CodeableConcept getLengthOfStay() { return lengthOfStay; }
    public String getEntryFrom() { return entryFrom; }
    public List<ServiceEpisode> getServiceEpisodes() { return List.copyOf(serviceEpisodes); }
    public ProjectExit getProjectExit() { return projectExit; }
    public Instant getCreatedAt() { return createdAt; }
    
    public boolean isActive() {
        return status == EnrollmentStatus.ACTIVE;
    }
    
    public boolean hasExited() {
        return status == EnrollmentStatus.EXITED && projectExit != null;
    }
    
    public int getServiceEpisodeCount() {
        return serviceEpisodes.size();
    }
    
    // HMIS Comparable Database methods
    public void updateHouseholdId(String householdId) {
        this.householdId = householdId;
    }
    
    public void updateHmisRelationshipToHoH(RelationshipToHeadOfHousehold relationship) {
        this.hmisRelationshipToHoH = relationship;
    }
    
    public void updateHmisPriorLivingSituation(PriorLivingSituation priorLiving) {
        this.hmisPriorLivingSituation = priorLiving;
    }
    
    public void updateHmisLengthOfStay(LengthOfStay lengthOfStay) {
        this.hmisLengthOfStay = lengthOfStay;
    }
    
    public void updateHmisDisablingCondition(DisablingCondition disablingCondition) {
        this.hmisDisablingCondition = disablingCondition;
    }
    
    // HMIS getters
    public String getHouseholdId() { 
        return householdId != null ? householdId : id.value().toString(); 
    }
    public RelationshipToHeadOfHousehold getHmisRelationshipToHoH() { 
        return hmisRelationshipToHoH != null ? hmisRelationshipToHoH : RelationshipToHeadOfHousehold.DATA_NOT_COLLECTED; 
    }
    public PriorLivingSituation getHmisPriorLivingSituation() { 
        return hmisPriorLivingSituation != null ? hmisPriorLivingSituation : PriorLivingSituation.DATA_NOT_COLLECTED; 
    }
    public LengthOfStay getHmisLengthOfStay() { 
        return hmisLengthOfStay != null ? hmisLengthOfStay : LengthOfStay.DATA_NOT_COLLECTED; 
    }
    public DisablingCondition getHmisDisablingCondition() { 
        return hmisDisablingCondition != null ? hmisDisablingCondition : DisablingCondition.DATA_NOT_COLLECTED; 
    }
    
    /**
     * Check if this enrollment is likely chronic homelessness
     * Based on HMIS chronic homelessness criteria
     */
    public boolean isPotentiallyChronicallyHomeless() {
        return hmisDisablingCondition == DisablingCondition.YES &&
               hmisPriorLivingSituation != null && hmisPriorLivingSituation.isLiterallyHomeless() &&
               hmisLengthOfStay != null && hmisLengthOfStay.isLongTerm();
    }
    
    /**
     * Determine if this is a family enrollment
     */
    public boolean isFamilyEnrollment() {
        return hmisRelationshipToHoH != null && 
               (hmisRelationshipToHoH.isHeadOfHousehold() || hmisRelationshipToHoH.isFamilyMember());
    }
    
    /**
     * Check if enrollment meets HMIS data quality standards
     */
    public boolean meetsHmisDataQuality() {
        return householdId != null &&
               hmisRelationshipToHoH != null && hmisRelationshipToHoH.isKnownRelationship() &&
               hmisPriorLivingSituation != null &&
               hmisLengthOfStay != null && hmisLengthOfStay.isKnownLength() &&
               hmisDisablingCondition != null && hmisDisablingCondition.isKnownStatus();
    }
    
    /**
     * Transition from TH to RRH in a Joint TH/RRH project
     * Creates a new RRH enrollment linked to this TH enrollment
     */
    public ProgramEnrollmentId transitionToRrh(UUID rrhProgramId, LocalDate moveInDate, 
                                               HmisProjectType rrhProjectType) {
        // Ensure enrollment is still active
        if (status != EnrollmentStatus.ACTIVE) {
            throw new IllegalStateException("Can only transition from active enrollment");
        }
        
        // Generate new enrollment ID for RRH
        ProgramEnrollmentId rrhEnrollmentId = ProgramEnrollmentId.generate();
        
        // Raise domain event for transition
        apply(new EnrollmentTransitionedToRrh(
            id.value(),
            rrhEnrollmentId.value(),
            clientId.value(),
            rrhProgramId,
            moveInDate,
            householdId, // Preserve household ID
            hmisRelationshipToHoH,
            hmisPriorLivingSituation,
            hmisLengthOfStay,
            hmisDisablingCondition,
            Instant.now()
        ));
        
        return rrhEnrollmentId;
    }
    
    /**
     * Create RRH enrollment from TH transition
     */
    public static ProgramEnrollment createFromTransition(
            ProgramEnrollmentId enrollmentId,
            ClientId clientId,
            UUID programId,
            UUID predecessorId,
            LocalDate enrollmentDate,
            LocalDate moveInDate,
            String householdId,
            RelationshipToHeadOfHousehold relationshipToHoH,
            PriorLivingSituation priorLivingSituation,
            LengthOfStay lengthOfStay,
            DisablingCondition disablingCondition,
            HmisProjectType projectType) {
        
        ProgramEnrollment enrollment = new ProgramEnrollment();
        enrollment.id = enrollmentId;
        enrollment.clientId = clientId;
        enrollment.programId = programId;
        enrollment.predecessorEnrollmentId = predecessorId;
        enrollment.enrollmentDate = enrollmentDate;
        enrollment.residentialMoveInDate = moveInDate;
        enrollment.householdId = householdId;
        enrollment.hmisRelationshipToHoH = relationshipToHoH;
        enrollment.hmisPriorLivingSituation = priorLivingSituation;
        enrollment.hmisLengthOfStay = lengthOfStay;
        enrollment.hmisDisablingCondition = disablingCondition;
        enrollment.projectType = projectType;
        enrollment.status = EnrollmentStatus.ACTIVE;
        enrollment.enrollmentPeriod = new Period(Instant.now(), null);
        enrollment.createdAt = Instant.now();
        
        return enrollment;
    }
    
    /**
     * Update residential move-in date for RRH enrollment
     */
    public void updateResidentialMoveInDate(LocalDate moveInDate) {
        if (projectType == null || (!projectType.isRapidRehousing() && !projectType.isJointThRrh())) {
            throw new IllegalStateException("Move-in date only applies to RRH enrollments");
        }
        
        if (moveInDate.isBefore(enrollmentDate)) {
            throw new IllegalArgumentException("Move-in date cannot be before enrollment date");
        }
        
        this.residentialMoveInDate = moveInDate;
        
        apply(new ResidentialMoveInDateUpdated(
            id.value(),
            moveInDate,
            Instant.now()
        ));
    }
    
    // Additional getters for new fields
    public UUID getPredecessorEnrollmentId() { return predecessorEnrollmentId; }
    public LocalDate getResidentialMoveInDate() { return residentialMoveInDate; }
    public HmisProjectType getProjectType() { return projectType; }
    
    public boolean isLinkedEnrollment() {
        return predecessorEnrollmentId != null;
    }
    
    public boolean hasResidentialMoveIn() {
        return residentialMoveInDate != null;
    }
    
    // Program Specific Data Elements methods
    public ProgramSpecificDataElements getProgramSpecificData() {
        if (programSpecificData == null) {
            programSpecificData = new ProgramSpecificDataElements();
        }
        return programSpecificData;
    }
    
    public void updateIncomeInformation(Integer totalMonthlyIncome, 
                                       List<org.haven.shared.vo.hmis.IncomeSource> incomeSources,
                                       LocalDate informationDate) {
        getProgramSpecificData().updateIncomeInformation(totalMonthlyIncome, incomeSources, informationDate);
    }
    
    public void updateNonCashBenefits(List<org.haven.shared.vo.hmis.NonCashBenefit> benefits, 
                                     LocalDate informationDate) {
        getProgramSpecificData().updateNonCashBenefits(benefits, informationDate);
    }
    
    public void updateHealthInsurance(List<org.haven.shared.vo.hmis.HealthInsurance> insurances, 
                                     LocalDate informationDate) {
        getProgramSpecificData().updateHealthInsurance(insurances, informationDate);
    }
    
    public void updateDisabilityInformation(org.haven.shared.vo.hmis.DisabilityType physicalDisability,
                                           org.haven.shared.vo.hmis.DisabilityType developmentalDisability,
                                           org.haven.shared.vo.hmis.DisabilityType chronicHealthCondition,
                                           org.haven.shared.vo.hmis.DisabilityType hivAids,
                                           org.haven.shared.vo.hmis.DisabilityType mentalHealthDisorder,
                                           org.haven.shared.vo.hmis.DisabilityType substanceUseDisorder,
                                           LocalDate informationDate) {
        getProgramSpecificData().updateDisabilityInformation(
            physicalDisability, developmentalDisability, chronicHealthCondition,
            hivAids, mentalHealthDisorder, substanceUseDisorder, informationDate);
    }
    
    public void updateDomesticViolence(org.haven.shared.vo.hmis.DomesticViolence domesticViolence, 
                                      LocalDate informationDate) {
        getProgramSpecificData().updateDomesticViolence(domesticViolence, informationDate);
    }
    
    public void updateCurrentLivingSituation(org.haven.shared.vo.hmis.CurrentLivingSituation situation) {
        getProgramSpecificData().updateCurrentLivingSituation(situation);
    }
    
    public void updateDateOfEngagement(LocalDate dateOfEngagement) {
        getProgramSpecificData().updateDateOfEngagement(dateOfEngagement);
    }
    
    public void updateBedNightDate(LocalDate bedNightDate) {
        getProgramSpecificData().updateBedNightDate(bedNightDate);
    }
    
    // Enhanced data quality checks including PSDE
    public boolean meetsEnhancedHmisDataQuality() {
        return meetsHmisDataQuality() && 
               (programSpecificData != null && programSpecificData.meetsHmisDataQuality());
    }
    
    public boolean hasCompleteAssessmentData() {
        return programSpecificData != null &&
               programSpecificData.hasCompleteIncomeInformation() &&
               programSpecificData.hasCompleteDisabilityInformation();
    }
    
    // Income and Benefits Lifecycle Management
    
    /**
     * Create income record at project start (HMIS required)
     */
    public void createStartIncomeRecord(org.haven.shared.vo.hmis.IncomeFromAnySource incomeFromAnySource, 
                                       String collectedBy) {
        // Check if START record already exists
        boolean hasStartRecord = incomeBenefitsRecords.stream()
            .anyMatch(record -> record.getRecordType() == org.haven.shared.vo.hmis.InformationDate.START_OF_PROJECT);
        
        if (hasStartRecord) {
            throw new IllegalStateException("START income record already exists for this enrollment");
        }
        
        IncomeBenefitsRecord startRecord = IncomeBenefitsRecord.createAtProjectStart(
            this.id, this.clientId, this.enrollmentDate, incomeFromAnySource, collectedBy);
        
        incomeBenefitsRecords.add(startRecord);
    }
    
    /**
     * Create income update record due to change in circumstances
     */
    public void createIncomeUpdateRecord(LocalDate changeDate, 
                                        org.haven.shared.vo.hmis.IncomeFromAnySource incomeFromAnySource,
                                        String collectedBy) {
        if (status != EnrollmentStatus.ACTIVE) {
            throw new IllegalStateException("Cannot update income for inactive enrollment");
        }
        
        if (changeDate.isBefore(enrollmentDate)) {
            throw new IllegalArgumentException("Change date cannot be before enrollment date");
        }
        
        IncomeBenefitsRecord updateRecord = IncomeBenefitsRecord.createUpdate(
            this.id, this.clientId, changeDate, incomeFromAnySource, collectedBy);
        
        incomeBenefitsRecords.add(updateRecord);
    }
    
    /**
     * Create annual assessment income record (HMIS required annually)
     */
    public void createAnnualIncomeAssessment(LocalDate assessmentDate,
                                            org.haven.shared.vo.hmis.IncomeFromAnySource incomeFromAnySource,
                                            String collectedBy) {
        if (status != EnrollmentStatus.ACTIVE) {
            throw new IllegalStateException("Cannot create annual assessment for inactive enrollment");
        }
        
        if (assessmentDate.isBefore(enrollmentDate.plusYears(1))) {
            throw new IllegalArgumentException("Annual assessment too early - must be at least 1 year after enrollment");
        }
        
        IncomeBenefitsRecord annualRecord = IncomeBenefitsRecord.createAnnualAssessment(
            this.id, this.clientId, assessmentDate, incomeFromAnySource, collectedBy);
        
        incomeBenefitsRecords.add(annualRecord);
    }
    
    /**
     * Create income record when minor client turns 18
     */
    public void createMinor18IncomeRecord(LocalDate birthdayDate,
                                         org.haven.shared.vo.hmis.IncomeFromAnySource incomeFromAnySource,
                                         String collectedBy) {
        if (status != EnrollmentStatus.ACTIVE) {
            throw new IllegalStateException("Cannot create minor-18 income record for inactive enrollment");
        }
        
        IncomeBenefitsRecord minor18Record = IncomeBenefitsRecord.createMinorTurning18(
            this.id, this.clientId, birthdayDate, incomeFromAnySource, collectedBy);
        
        incomeBenefitsRecords.add(minor18Record);
    }
    
    /**
     * Create income record at project exit (HMIS required)
     */
    public void createExitIncomeRecord(LocalDate exitDate,
                                      org.haven.shared.vo.hmis.IncomeFromAnySource incomeFromAnySource,
                                      String collectedBy) {
        // Check if EXIT record already exists
        boolean hasExitRecord = incomeBenefitsRecords.stream()
            .anyMatch(record -> record.getRecordType() == org.haven.shared.vo.hmis.InformationDate.EXIT);
        
        if (hasExitRecord) {
            throw new IllegalStateException("EXIT income record already exists for this enrollment");
        }
        
        IncomeBenefitsRecord exitRecord = IncomeBenefitsRecord.createAtProjectExit(
            this.id, this.clientId, exitDate, incomeFromAnySource, collectedBy);
        
        incomeBenefitsRecords.add(exitRecord);
    }
    
    /**
     * Get the most recent income record
     */
    public IncomeBenefitsRecord getMostRecentIncomeRecord() {
        return incomeBenefitsRecords.stream()
            .max((r1, r2) -> r1.getInformationDate().compareTo(r2.getInformationDate()))
            .orElse(null);
    }
    
    /**
     * Get income record of specific type
     */
    public IncomeBenefitsRecord getIncomeRecord(org.haven.shared.vo.hmis.InformationDate recordType) {
        return incomeBenefitsRecords.stream()
            .filter(record -> record.getRecordType() == recordType)
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Check if enrollment needs annual income assessment
     */
    public boolean needsAnnualIncomeAssessment() {
        if (status != EnrollmentStatus.ACTIVE) {
            return false;
        }
        
        LocalDate oneYearAgo = LocalDate.now().minusYears(1);
        
        // Check if enrollment started more than a year ago
        if (enrollmentDate.isAfter(oneYearAgo)) {
            return false;
        }
        
        // Check if there's an annual assessment in the last year
        return incomeBenefitsRecords.stream()
            .noneMatch(record -> 
                record.getRecordType() == org.haven.shared.vo.hmis.InformationDate.ANNUAL_ASSESSMENT &&
                record.getInformationDate().isAfter(oneYearAgo));
    }
    
    /**
     * Check if enrollment has required START income record
     */
    public boolean hasStartIncomeRecord() {
        return incomeBenefitsRecords.stream()
            .anyMatch(record -> record.getRecordType() == org.haven.shared.vo.hmis.InformationDate.START_OF_PROJECT);
    }
    
    /**
     * Check if enrollment meets HMIS income data compliance
     */
    public boolean meetsIncomeDataCompliance() {
        // Must have START record
        if (!hasStartIncomeRecord()) {
            return false;
        }
        
        // If active for more than a year, must have annual assessment
        if (needsAnnualIncomeAssessment()) {
            return false;
        }
        
        // If exited, must have EXIT record
        if (status == EnrollmentStatus.EXITED) {
            return incomeBenefitsRecords.stream()
                .anyMatch(record -> record.getRecordType() == org.haven.shared.vo.hmis.InformationDate.EXIT);
        }
        
        // All income records must meet data quality
        return incomeBenefitsRecords.stream()
            .allMatch(IncomeBenefitsRecord::meetsDataQuality);
    }
    
    /**
     * Get all income records for this enrollment
     */
    public List<IncomeBenefitsRecord> getIncomeBenefitsRecords() {
        return List.copyOf(incomeBenefitsRecords);
    }
    
    /**
     * Update existing income record with source details
     */
    public void updateIncomeRecordSources(IncomeBenefitsRecord record, 
                                         org.haven.shared.vo.hmis.IncomeSource source,
                                         org.haven.shared.vo.hmis.DisabilityType hasSource,
                                         Integer amount) {
        if (!incomeBenefitsRecords.contains(record)) {
            throw new IllegalArgumentException("Income record does not belong to this enrollment");
        }
        
        record.updateIncomeSource(source, hasSource, amount);
    }
    
    /**
     * Update total monthly income for a record
     */
    public void updateIncomeRecordTotal(IncomeBenefitsRecord record, Integer totalAmount) {
        if (!incomeBenefitsRecords.contains(record)) {
            throw new IllegalArgumentException("Income record does not belong to this enrollment");
        }
        
        record.updateTotalMonthlyIncome(totalAmount);
    }
    
    // Physical Disability Lifecycle Management (UDE 3.08)
    
    /**
     * Create physical disability record at project start (HMIS required)
     */
    public void createStartPhysicalDisabilityRecord(HmisFivePointResponse physicalDisability, 
                                                   String collectedBy) {
        // Check if PROJECT_START record already exists
        boolean hasStartRecord = physicalDisabilityRecords.stream()
            .anyMatch(record -> record.getStage() == DataCollectionStage.PROJECT_START && !record.isCorrection());
        
        if (hasStartRecord) {
            throw new IllegalStateException("PROJECT_START physical disability record already exists for this enrollment");
        }
        
        PhysicalDisabilityRecord startRecord = PhysicalDisabilityRecord.createAtProjectStart(
            this.id, this.clientId, this.enrollmentDate, physicalDisability, collectedBy);
        
        physicalDisabilityRecords.add(startRecord);
        
        // Update HMIS disabling condition if applicable and configuration allows
        updateDisablingConditionIfApplicable(startRecord);
    }
    
    /**
     * Create physical disability update record due to change in circumstances
     */
    public void createPhysicalDisabilityUpdateRecord(LocalDate changeDate, 
                                                   HmisFivePointResponse physicalDisability,
                                                   String collectedBy) {
        if (status != EnrollmentStatus.ACTIVE) {
            throw new IllegalStateException("Cannot update physical disability for inactive enrollment");
        }
        
        if (changeDate.isBefore(enrollmentDate)) {
            throw new IllegalArgumentException("Change date cannot be before enrollment date");
        }
        
        PhysicalDisabilityRecord updateRecord = PhysicalDisabilityRecord.createUpdate(
            this.id, this.clientId, changeDate, physicalDisability, collectedBy);
        
        physicalDisabilityRecords.add(updateRecord);
        
        // Update HMIS disabling condition if applicable
        updateDisablingConditionIfApplicable(updateRecord);
    }
    
    /**
     * Create physical disability record at project exit (HMIS required)
     */
    public void createExitPhysicalDisabilityRecord(LocalDate exitDate,
                                                 HmisFivePointResponse physicalDisability,
                                                 String collectedBy) {
        // Check if PROJECT_EXIT record already exists
        boolean hasExitRecord = physicalDisabilityRecords.stream()
            .anyMatch(record -> record.getStage() == DataCollectionStage.PROJECT_EXIT && !record.isCorrection());
        
        if (hasExitRecord) {
            throw new IllegalStateException("PROJECT_EXIT physical disability record already exists for this enrollment");
        }
        
        PhysicalDisabilityRecord exitRecord = PhysicalDisabilityRecord.createAtProjectExit(
            this.id, this.clientId, exitDate, physicalDisability, collectedBy);
        
        physicalDisabilityRecords.add(exitRecord);
        
        // Update HMIS disabling condition if applicable
        updateDisablingConditionIfApplicable(exitRecord);
    }
    
    /**
     * Create correction record for an existing physical disability record
     */
    public void correctPhysicalDisabilityRecord(PhysicalDisabilityRecord originalRecord,
                                              HmisFivePointResponse physicalDisability,
                                              String collectedBy) {
        if (!physicalDisabilityRecords.contains(originalRecord)) {
            throw new IllegalArgumentException("Physical disability record does not belong to this enrollment");
        }
        
        PhysicalDisabilityRecord correction = PhysicalDisabilityRecord.createCorrection(
            originalRecord, physicalDisability, collectedBy);
        
        physicalDisabilityRecords.add(correction);
        
        // Update HMIS disabling condition if applicable
        updateDisablingConditionIfApplicable(correction);
    }
    
    /**
     * Update physical expected long-term response for a record
     */
    public void updatePhysicalExpectedLongTerm(PhysicalDisabilityRecord record,
                                             HmisFivePointResponse expectedLongTerm) {
        if (!physicalDisabilityRecords.contains(record)) {
            throw new IllegalArgumentException("Physical disability record does not belong to this enrollment");
        }
        
        record.updatePhysicalExpectedLongTerm(expectedLongTerm);
        
        // Update HMIS disabling condition if applicable
        updateDisablingConditionIfApplicable(record);
    }
    
    /**
     * Update both physical disability responses for a record
     */
    public void updatePhysicalDisabilityStatus(PhysicalDisabilityRecord record,
                                             HmisFivePointResponse physicalDisability,
                                             HmisFivePointResponse expectedLongTerm) {
        if (!physicalDisabilityRecords.contains(record)) {
            throw new IllegalArgumentException("Physical disability record does not belong to this enrollment");
        }
        
        record.updatePhysicalDisabilityStatus(physicalDisability, expectedLongTerm);
        
        // Update HMIS disabling condition if applicable
        updateDisablingConditionIfApplicable(record);
    }
    
    /**
     * UDE 3.08 derivation logic: Update HMIS disabling condition based on physical disability
     * When physical_disability=YES and physical_expected_long_term=YES, set hmisDisablingCondition=YES
     * This is configurable and can be disabled if needed
     */
    private void updateDisablingConditionIfApplicable(PhysicalDisabilityRecord record) {
        // TODO: Make this configurable via application properties
        boolean enableUdeDerivation = true; // Default: enabled
        
        if (!enableUdeDerivation) {
            return;
        }
        
        if (record.indicatesDisablingCondition()) {
            this.hmisDisablingCondition = DisablingCondition.YES;
        }
    }
    
    /**
     * Get the most recent physical disability record
     */
    public PhysicalDisabilityRecord getMostRecentPhysicalDisabilityRecord() {
        return physicalDisabilityRecords.stream()
            .filter(record -> !record.isCorrection()) // Exclude corrections
            .max((r1, r2) -> r1.getInformationDate().compareTo(r2.getInformationDate()))
            .orElse(null);
    }
    
    /**
     * Get physical disability record of specific stage
     */
    public PhysicalDisabilityRecord getPhysicalDisabilityRecord(DataCollectionStage stage) {
        return physicalDisabilityRecords.stream()
            .filter(record -> record.getStage() == stage && !record.isCorrection())
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Get latest effective physical disability record (accounts for corrections)
     */
    public PhysicalDisabilityRecord getLatestEffectivePhysicalDisabilityRecord() {
        return physicalDisabilityRecords.stream()
            .sorted((r1, r2) -> {
                int dateCompare = r2.getInformationDate().compareTo(r1.getInformationDate());
                if (dateCompare != 0) return dateCompare;
                // If same date, prefer corrections (they're more recent)
                return Boolean.compare(r2.isCorrection(), r1.isCorrection());
            })
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Check if enrollment has required PROJECT_START physical disability record
     */
    public boolean hasStartPhysicalDisabilityRecord() {
        return physicalDisabilityRecords.stream()
            .anyMatch(record -> record.getStage() == DataCollectionStage.PROJECT_START);
    }
    
    /**
     * Check if enrollment has PROJECT_EXIT physical disability record
     */
    public boolean hasExitPhysicalDisabilityRecord() {
        return physicalDisabilityRecords.stream()
            .anyMatch(record -> record.getStage() == DataCollectionStage.PROJECT_EXIT);
    }
    
    /**
     * Check if enrollment meets HMIS physical disability data compliance
     */
    public boolean meetsPhysicalDisabilityDataCompliance() {
        // Must have PROJECT_START record
        if (!hasStartPhysicalDisabilityRecord()) {
            return false;
        }
        
        // If exited, must have PROJECT_EXIT record
        if (status == EnrollmentStatus.EXITED && !hasExitPhysicalDisabilityRecord()) {
            return false;
        }
        
        // All physical disability records must meet data quality
        return physicalDisabilityRecords.stream()
            .allMatch(PhysicalDisabilityRecord::meetsDataQuality);
    }
    
    /**
     * Check if client currently has a disabling physical condition
     * Based on latest effective record
     */
    public boolean hasDisablingPhysicalCondition() {
        PhysicalDisabilityRecord latest = getLatestEffectivePhysicalDisabilityRecord();
        return latest != null && latest.indicatesDisablingCondition();
    }
    
    /**
     * Check if client currently has any physical disability
     * Based on latest effective record
     */
    public boolean hasPhysicalDisability() {
        PhysicalDisabilityRecord latest = getLatestEffectivePhysicalDisabilityRecord();
        return latest != null && latest.hasPhysicalDisability();
    }
    
    /**
     * Get all physical disability records for this enrollment
     */
    public List<PhysicalDisabilityRecord> getPhysicalDisabilityRecords() {
        return List.copyOf(physicalDisabilityRecords);
    }
    
    // =============================================================================
    // Disability Records Lifecycle Management (UDE 3.09-3.13)
    // =============================================================================
    
    /**
     * Create disability record at project start
     */
    public void createStartDisabilityRecord(DisabilityKind disabilityKind, 
                                           HmisFivePoint hasDisability, 
                                           String collectedBy) {
        // Check if PROJECT_START record already exists for this disability kind
        boolean hasStartRecord = disabilityRecords.stream()
            .anyMatch(record -> record.getDisabilityKind() == disabilityKind && 
                               record.getStage() == DataCollectionStage.PROJECT_START && 
                               !record.isCorrection());
        
        if (hasStartRecord) {
            throw new IllegalStateException("PROJECT_START " + disabilityKind.getDisplayName() + 
                " record already exists for this enrollment");
        }
        
        DisabilityRecord startRecord = DisabilityRecord.createAtProjectStart(
            this.id, this.clientId, this.enrollmentDate, disabilityKind, hasDisability, collectedBy);
        
        disabilityRecords.add(startRecord);
        
        // Update HMIS disabling condition if applicable
        updateDisablingConditionFromDisability(startRecord);
    }
    
    /**
     * Create disability update record due to change in circumstances
     */
    public void createDisabilityUpdateRecord(DisabilityKind disabilityKind,
                                            LocalDate changeDate, 
                                            HmisFivePoint hasDisability,
                                            String collectedBy) {
        if (status != EnrollmentStatus.ACTIVE) {
            throw new IllegalStateException("Cannot update disability for inactive enrollment");
        }
        
        if (changeDate.isBefore(enrollmentDate)) {
            throw new IllegalArgumentException("Change date cannot be before enrollment date");
        }
        
        DisabilityRecord updateRecord = DisabilityRecord.createUpdate(
            this.id, this.clientId, changeDate, disabilityKind, hasDisability, collectedBy);
        
        disabilityRecords.add(updateRecord);
        
        // Update HMIS disabling condition if applicable
        updateDisablingConditionFromDisability(updateRecord);
    }
    
    /**
     * Create disability record at project exit
     */
    public void createExitDisabilityRecord(DisabilityKind disabilityKind,
                                          LocalDate exitDate,
                                          HmisFivePoint hasDisability,
                                          String collectedBy) {
        // Check if PROJECT_EXIT record already exists for this disability kind
        boolean hasExitRecord = disabilityRecords.stream()
            .anyMatch(record -> record.getDisabilityKind() == disabilityKind && 
                               record.getStage() == DataCollectionStage.PROJECT_EXIT && 
                               !record.isCorrection());
        
        if (hasExitRecord) {
            throw new IllegalStateException("PROJECT_EXIT " + disabilityKind.getDisplayName() + 
                " record already exists for this enrollment");
        }
        
        DisabilityRecord exitRecord = DisabilityRecord.createAtProjectExit(
            this.id, this.clientId, exitDate, disabilityKind, hasDisability, collectedBy);
        
        disabilityRecords.add(exitRecord);
        
        // Update HMIS disabling condition if applicable
        updateDisablingConditionFromDisability(exitRecord);
    }
    
    /**
     * Create correction record for an existing disability record
     */
    public void correctDisabilityRecord(DisabilityRecord originalRecord,
                                       HmisFivePoint hasDisability,
                                       String collectedBy) {
        if (!disabilityRecords.contains(originalRecord)) {
            throw new IllegalArgumentException("Disability record does not belong to this enrollment");
        }
        
        DisabilityRecord correction = DisabilityRecord.createCorrection(
            originalRecord, hasDisability, collectedBy);
        
        disabilityRecords.add(correction);
        
        // Update HMIS disabling condition if applicable
        updateDisablingConditionFromDisability(correction);
    }
    
    /**
     * Update expected long-term response for a disability record
     */
    public void updateDisabilityExpectedLongTerm(DisabilityRecord record,
                                                HmisFivePoint expectedLongTerm) {
        if (!disabilityRecords.contains(record)) {
            throw new IllegalArgumentException("Disability record does not belong to this enrollment");
        }
        
        record.updateExpectedLongTerm(expectedLongTerm);
        
        // Update HMIS disabling condition if applicable
        updateDisablingConditionFromDisability(record);
    }
    
    /**
     * Get the most recent disability record for a specific kind
     */
    public DisabilityRecord getMostRecentDisabilityRecord(DisabilityKind disabilityKind) {
        return disabilityRecords.stream()
            .filter(record -> record.getDisabilityKind() == disabilityKind && !record.isCorrection())
            .max((r1, r2) -> r1.getInformationDate().compareTo(r2.getInformationDate()))
            .orElse(null);
    }
    
    /**
     * Get disability record of specific stage and kind
     */
    public DisabilityRecord getDisabilityRecord(DisabilityKind disabilityKind, DataCollectionStage stage) {
        return disabilityRecords.stream()
            .filter(record -> record.getDisabilityKind() == disabilityKind && 
                             record.getStage() == stage && 
                             !record.isCorrection())
            .findFirst()
            .orElse(null);
    }
    
    /**
     * Check if enrollment has required PROJECT_START records for all disability kinds
     */
    public boolean hasStartDisabilityRecords() {
        for (DisabilityKind kind : DisabilityKind.values()) {
            boolean hasStart = disabilityRecords.stream()
                .anyMatch(record -> record.getDisabilityKind() == kind && 
                                   record.getStage() == DataCollectionStage.PROJECT_START);
            if (!hasStart) return false;
        }
        return true;
    }
    
    /**
     * Check if client currently has any disabling condition from any disability
     */
    public boolean hasAnyDisablingCondition() {
        // Check physical disability
        if (hasDisablingPhysicalCondition()) return true;
        
        // Check other disability types
        for (DisabilityKind kind : DisabilityKind.values()) {
            if (kind != DisabilityKind.PHYSICAL) { // Physical already checked above
                DisabilityRecord latest = getMostRecentDisabilityRecord(kind);
                if (latest != null && latest.indicatesDisablingCondition()) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    /**
     * Update HMIS disabling condition based on disability records
     */
    private void updateDisablingConditionFromDisability(DisabilityRecord record) {
        // TODO: Make this configurable via application properties
        boolean enableUdeDerivation = true; // Default: enabled
        
        if (!enableUdeDerivation) {
            return;
        }
        
        if (record.indicatesDisablingCondition()) {
            this.hmisDisablingCondition = DisablingCondition.YES;
        }
    }
    
    /**
     * Get all disability records for this enrollment
     */
    public List<DisabilityRecord> getDisabilityRecords() {
        return List.copyOf(disabilityRecords);
    }
    
    // =============================================================================
    // Domestic Violence Records Lifecycle Management (HMIS 4.11)
    // =============================================================================
    
    /**
     * Create DV record at project start
     */
    public void createStartDvRecord(HmisFivePoint dvHistory, String collectedBy) {
        // Check if PROJECT_START record already exists
        boolean hasStartRecord = dvRecords.stream()
            .anyMatch(record -> record.getStage() == DataCollectionStage.PROJECT_START && !record.isCorrection());
        
        if (hasStartRecord) {
            throw new IllegalStateException("PROJECT_START DV record already exists for this enrollment");
        }
        
        DvRecord startRecord = DvRecord.createAtProjectStart(
            this.id, this.clientId, this.enrollmentDate, dvHistory, collectedBy);
        
        dvRecords.add(startRecord);
    }
    
    /**
     * Create DV update record due to change in circumstances
     */
    public void createDvUpdateRecord(LocalDate changeDate, 
                                    HmisFivePoint dvHistory,
                                    String collectedBy) {
        if (status != EnrollmentStatus.ACTIVE) {
            throw new IllegalStateException("Cannot update DV for inactive enrollment");
        }
        
        if (changeDate.isBefore(enrollmentDate)) {
            throw new IllegalArgumentException("Change date cannot be before enrollment date");
        }
        
        DvRecord updateRecord = DvRecord.createUpdate(
            this.id, this.clientId, changeDate, dvHistory, collectedBy);
        
        dvRecords.add(updateRecord);
    }
    
    /**
     * Create DV record at project exit
     */
    public void createExitDvRecord(LocalDate exitDate, HmisFivePoint dvHistory, String collectedBy) {
        // Check if PROJECT_EXIT record already exists
        boolean hasExitRecord = dvRecords.stream()
            .anyMatch(record -> record.getStage() == DataCollectionStage.PROJECT_EXIT && !record.isCorrection());
        
        if (hasExitRecord) {
            throw new IllegalStateException("PROJECT_EXIT DV record already exists for this enrollment");
        }
        
        DvRecord exitRecord = DvRecord.createAtProjectExit(
            this.id, this.clientId, exitDate, dvHistory, collectedBy);
        
        dvRecords.add(exitRecord);
    }
    
    /**
     * Update DV record with fleeing status and recency
     */
    public void updateDvRecord(DvRecord record, 
                              HmisFivePoint currentlyFleeing, 
                              DomesticViolenceRecency whenExperienced) {
        if (!dvRecords.contains(record)) {
            throw new IllegalArgumentException("DV record does not belong to this enrollment");
        }
        
        record.updateDvStatus(record.getDvHistory(), currentlyFleeing, whenExperienced);
    }
    
    /**
     * Get the most recent DV record
     */
    public DvRecord getMostRecentDvRecord() {
        return dvRecords.stream()
            .filter(record -> !record.isCorrection())
            .max((r1, r2) -> r1.getInformationDate().compareTo(r2.getInformationDate()))
            .orElse(null);
    }
    
    /**
     * Check if client requires enhanced safety measures
     */
    public boolean requiresEnhancedSafety() {
        DvRecord latest = getMostRecentDvRecord();
        return latest != null && latest.requiresEnhancedSafety();
    }
    
    /**
     * Check if client is currently fleeing DV
     */
    public boolean isCurrentlyFleeingDv() {
        DvRecord latest = getMostRecentDvRecord();
        return latest != null && latest.isCurrentlyFleeing();
    }
    
    /**
     * Get all DV records for this enrollment
     */
    public List<DvRecord> getDvRecords() {
        return List.copyOf(dvRecords);
    }
    
    // =============================================================================
    // Current Living Situation Management
    // =============================================================================
    
    /**
     * Record a current living situation contact
     */
    public void recordCurrentLivingSituation(LocalDate contactDate,
                                           PriorLivingSituation livingSituation,
                                           String locationDescription,
                                           String verifiedBy,
                                           String createdBy) {
        if (contactDate.isBefore(enrollmentDate)) {
            throw new IllegalArgumentException("Contact date cannot be before enrollment date");
        }
        
        if (contactDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Contact date cannot be in the future");
        }
        
        CurrentLivingSituation cls = CurrentLivingSituation.createWithDetails(
            this.id, this.clientId, contactDate, livingSituation, 
            locationDescription, verifiedBy, createdBy);
        
        currentLivingSituations.add(cls);
    }
    
    /**
     * Get the most recent living situation contact
     */
    public CurrentLivingSituation getMostRecentLivingSituation() {
        return currentLivingSituations.stream()
            .max((c1, c2) -> c1.getContactDate().compareTo(c2.getContactDate()))
            .orElse(null);
    }
    
    /**
     * Get all current living situation contacts
     */
    public List<CurrentLivingSituation> getCurrentLivingSituations() {
        return List.copyOf(currentLivingSituations);
    }
    
    // =============================================================================
    // Date of Engagement Management  
    // =============================================================================
    
    /**
     * Set date of engagement
     */
    public void setDateOfEngagement(LocalDate engagementDate, String createdBy) {
        if (dateOfEngagement != null && !dateOfEngagement.isCorrection()) {
            throw new IllegalStateException("Date of engagement already exists. Use correctDateOfEngagement instead.");
        }
        
        dateOfEngagement = DateOfEngagement.create(this.id, this.clientId, engagementDate, createdBy);
    }
    
    /**
     * Correct existing date of engagement
     */
    public void correctDateOfEngagement(LocalDate newEngagementDate, String createdBy) {
        if (dateOfEngagement == null) {
            throw new IllegalStateException("No existing date of engagement to correct");
        }
        
        dateOfEngagement = DateOfEngagement.createCorrection(dateOfEngagement, newEngagementDate, createdBy);
    }
    
    /**
     * Get current date of engagement
     */
    public DateOfEngagement getDateOfEngagement() {
        return dateOfEngagement;
    }
    
    /**
     * Check if engagement date is set
     */
    public boolean hasDateOfEngagement() {
        return dateOfEngagement != null;
    }
    
    // =============================================================================
    // Bed Night Management (ES-NbN projects)
    // =============================================================================
    
    /**
     * Add a single bed night
     */
    public void addBedNight(LocalDate bedNightDate, String createdBy) {
        if (bedNightDate.isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("Bed night date cannot be in the future");
        }
        
        if (bedNightDate.isBefore(enrollmentDate)) {
            throw new IllegalArgumentException("Bed night date cannot be before enrollment date");
        }
        
        // Check for duplicate
        boolean exists = bedNights.stream()
            .anyMatch(bn -> bn.getBedNightDate().equals(bedNightDate));
        
        if (exists) {
            throw new IllegalArgumentException("Bed night already exists for date: " + bedNightDate);
        }
        
        BedNight bedNight = BedNight.create(this.id, this.clientId, bedNightDate, createdBy);
        bedNights.add(bedNight);
    }
    
    /**
     * Add multiple bed nights in range
     */
    public void addBedNightRange(LocalDate startDate, LocalDate endDate, String createdBy) {
        if (startDate.isAfter(endDate)) {
            throw new IllegalArgumentException("Start date must be before or equal to end date");
        }
        
        LocalDate current = startDate;
        while (!current.isAfter(endDate)) {
            try {
                addBedNight(current, createdBy);
            } catch (IllegalArgumentException e) {
                // Skip duplicates, continue with next date
            }
            current = current.plusDays(1);
        }
    }
    
    /**
     * Remove bed night for specific date
     */
    public void removeBedNight(LocalDate bedNightDate) {
        bedNights.removeIf(bn -> bn.getBedNightDate().equals(bedNightDate));
    }
    
    /**
     * Get total bed nights count
     */
    public int getBedNightCount() {
        return bedNights.size();
    }
    
    /**
     * Get all bed nights for this enrollment
     */
    public List<BedNight> getBedNights() {
        return List.copyOf(bedNights);
    }
}