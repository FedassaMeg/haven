package org.haven.programenrollment.application.services;

import org.haven.programenrollment.domain.*;
import org.haven.programenrollment.infrastructure.persistence.*;
import org.haven.clientprofile.domain.ClientId;
import org.haven.shared.vo.hmis.*;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Lazy;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/**
 * Application Service for HMIS-compliant Income and Benefits lifecycle management
 * Handles automatic creation of required income records per HMIS Data Standards
 */
@Service
@Transactional
public class IncomeLifecycleService {
    
    private final JpaProgramEnrollmentRepository enrollmentRepository;
    private final JpaIncomeBenefitsRepository incomeRepository;
    
    public IncomeLifecycleService(
            @Lazy JpaProgramEnrollmentRepository enrollmentRepository,
            @Lazy JpaIncomeBenefitsRepository incomeRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.incomeRepository = incomeRepository;
    }
    
    /**
     * Create required START income record when client enrolls
     */
    public IncomeBenefitsRecord createStartIncomeRecord(
            UUID enrollmentId,
            IncomeFromAnySource incomeFromAnySource,
            String collectedBy) {
        
        // Load enrollment aggregate
        JpaProgramEnrollmentEntity enrollmentEntity = enrollmentRepository.findById(enrollmentId)
            .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + enrollmentId));
        
        ProgramEnrollment enrollment = enrollmentEntity.toDomainObject();
        
        // Create START record through aggregate
        enrollment.createStartIncomeRecord(incomeFromAnySource, collectedBy);
        
        // Get the created record
        IncomeBenefitsRecord startRecord = enrollment.getIncomeRecord(InformationDate.START_OF_PROJECT);
        
        // Persist the record
        JpaIncomeBenefitsEntity incomeEntity = new JpaIncomeBenefitsEntity(startRecord);
        incomeRepository.save(incomeEntity);
        
        return startRecord;
    }
    
    /**
     * Create income update record due to change in circumstances
     */
    public IncomeBenefitsRecord createIncomeUpdate(
            UUID enrollmentId,
            LocalDate changeDate,
            IncomeFromAnySource incomeFromAnySource,
            String collectedBy) {
        
        JpaProgramEnrollmentEntity enrollmentEntity = enrollmentRepository.findById(enrollmentId)
            .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + enrollmentId));
        
        ProgramEnrollment enrollment = enrollmentEntity.toDomainObject();
        
        // Create UPDATE record
        enrollment.createIncomeUpdateRecord(changeDate, incomeFromAnySource, collectedBy);
        
        // Get the most recent record (should be the update we just created)
        IncomeBenefitsRecord updateRecord = enrollment.getMostRecentIncomeRecord();
        
        // Persist the record
        JpaIncomeBenefitsEntity incomeEntity = new JpaIncomeBenefitsEntity(updateRecord);
        incomeRepository.save(incomeEntity);
        
        return updateRecord;
    }
    
    /**
     * Create annual income assessment (HMIS required annually)
     */
    public IncomeBenefitsRecord createAnnualAssessment(
            UUID enrollmentId,
            LocalDate assessmentDate,
            IncomeFromAnySource incomeFromAnySource,
            String collectedBy) {
        
        JpaProgramEnrollmentEntity enrollmentEntity = enrollmentRepository.findById(enrollmentId)
            .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + enrollmentId));
        
        ProgramEnrollment enrollment = enrollmentEntity.toDomainObject();
        
        // Create ANNUAL record
        enrollment.createAnnualIncomeAssessment(assessmentDate, incomeFromAnySource, collectedBy);
        
        // Get the annual record
        IncomeBenefitsRecord annualRecord = enrollment.getIncomeRecord(InformationDate.ANNUAL_ASSESSMENT);
        
        // Persist the record
        JpaIncomeBenefitsEntity incomeEntity = new JpaIncomeBenefitsEntity(annualRecord);
        incomeRepository.save(incomeEntity);
        
        return annualRecord;
    }
    
    /**
     * Create income record when minor turns 18
     */
    public IncomeBenefitsRecord createMinor18Record(
            UUID enrollmentId,
            LocalDate birthdayDate,
            IncomeFromAnySource incomeFromAnySource,
            String collectedBy) {
        
        JpaProgramEnrollmentEntity enrollmentEntity = enrollmentRepository.findById(enrollmentId)
            .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + enrollmentId));
        
        ProgramEnrollment enrollment = enrollmentEntity.toDomainObject();
        
        // Create MINOR18 record
        enrollment.createMinor18IncomeRecord(birthdayDate, incomeFromAnySource, collectedBy);
        
        // Get the minor18 record
        IncomeBenefitsRecord minor18Record = enrollment.getMostRecentIncomeRecord();
        
        // Persist the record
        JpaIncomeBenefitsEntity incomeEntity = new JpaIncomeBenefitsEntity(minor18Record);
        incomeRepository.save(incomeEntity);
        
        return minor18Record;
    }
    
    /**
     * Create required EXIT income record when client exits
     */
    public IncomeBenefitsRecord createExitIncomeRecord(
            UUID enrollmentId,
            LocalDate exitDate,
            IncomeFromAnySource incomeFromAnySource,
            String collectedBy) {
        
        JpaProgramEnrollmentEntity enrollmentEntity = enrollmentRepository.findById(enrollmentId)
            .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + enrollmentId));
        
        ProgramEnrollment enrollment = enrollmentEntity.toDomainObject();
        
        // Create EXIT record
        enrollment.createExitIncomeRecord(exitDate, incomeFromAnySource, collectedBy);
        
        // Get the exit record
        IncomeBenefitsRecord exitRecord = enrollment.getIncomeRecord(InformationDate.EXIT);
        
        // Persist the record
        JpaIncomeBenefitsEntity incomeEntity = new JpaIncomeBenefitsEntity(exitRecord);
        incomeRepository.save(incomeEntity);
        
        return exitRecord;
    }
    
    /**
     * Update income record with individual source amounts
     */
    public void updateIncomeSource(
            UUID recordId,
            IncomeSource source,
            DisabilityType hasSource,
            Integer amount) {
        
        JpaIncomeBenefitsEntity incomeEntity = incomeRepository.findById(recordId)
            .orElseThrow(() -> new IllegalArgumentException("Income record not found: " + recordId));
        
        IncomeBenefitsRecord incomeRecord = incomeEntity.toDomainObject();
        
        // Update the source
        incomeRecord.updateIncomeSource(source, hasSource, amount);
        
        // Save updated entity
        JpaIncomeBenefitsEntity updatedEntity = new JpaIncomeBenefitsEntity(incomeRecord);
        incomeRepository.save(updatedEntity);
    }
    
    /**
     * Update "Other" income source with specify text
     */
    public void updateOtherIncomeSource(
            UUID recordId,
            DisabilityType hasSource,
            Integer amount,
            String specify) {
        
        JpaIncomeBenefitsEntity incomeEntity = incomeRepository.findById(recordId)
            .orElseThrow(() -> new IllegalArgumentException("Income record not found: " + recordId));
        
        IncomeBenefitsRecord incomeRecord = incomeEntity.toDomainObject();
        
        // Update the other source with specify text
        incomeRecord.updateOtherIncomeSource(hasSource, amount, specify);
        
        // Save updated entity
        JpaIncomeBenefitsEntity updatedEntity = new JpaIncomeBenefitsEntity(incomeRecord);
        incomeRepository.save(updatedEntity);
    }
    
    /**
     * Update total monthly income for a record
     */
    public void updateTotalMonthlyIncome(UUID recordId, Integer totalAmount) {
        JpaIncomeBenefitsEntity incomeEntity = incomeRepository.findById(recordId)
            .orElseThrow(() -> new IllegalArgumentException("Income record not found: " + recordId));
        
        IncomeBenefitsRecord incomeRecord = incomeEntity.toDomainObject();
        
        // Update total
        incomeRecord.updateTotalMonthlyIncome(totalAmount);
        
        // Save updated entity
        JpaIncomeBenefitsEntity updatedEntity = new JpaIncomeBenefitsEntity(incomeRecord);
        incomeRepository.save(updatedEntity);
    }
    
    /**
     * Get all income records for an enrollment
     */
    @Transactional(readOnly = true)
    public List<IncomeBenefitsRecord> getIncomeRecordsForEnrollment(UUID enrollmentId) {
        List<JpaIncomeBenefitsEntity> entities = incomeRepository
            .findByEnrollmentIdOrderByInformationDateDesc(enrollmentId);
        
        return entities.stream()
            .map(JpaIncomeBenefitsEntity::toDomainObject)
            .toList();
    }
    
    /**
     * Get the most recent income record for an enrollment
     */
    @Transactional(readOnly = true)
    public IncomeBenefitsRecord getMostRecentIncomeRecord(UUID enrollmentId) {
        return incomeRepository.findFirstByEnrollmentIdOrderByInformationDateDesc(enrollmentId)
            .map(JpaIncomeBenefitsEntity::toDomainObject)
            .orElse(null);
    }
    
    /**
     * Find enrollments that need annual income assessments
     */
    @Transactional(readOnly = true)
    public List<UUID> findEnrollmentsNeedingAnnualAssessment() {
        LocalDate oneYearAgo = LocalDate.now().minusYears(1);
        return incomeRepository.findEnrollmentIdsNeedingAnnualAssessment(oneYearAgo);
    }
    
    /**
     * Find clients who turned 18 and need income assessment
     */
    @Transactional(readOnly = true)
    public List<UUID> findClientsNeedingMinor18Assessment(List<UUID> clientIds, LocalDate since18thBirthday) {
        return incomeRepository.findClientsNeedingMinor18Assessment(clientIds, since18thBirthday);
    }
    
    /**
     * Check if enrollment has required START record
     */
    @Transactional(readOnly = true)
    public boolean hasStartIncomeRecord(UUID enrollmentId) {
        return incomeRepository.existsByEnrollmentIdAndRecordType(enrollmentId, "START");
    }
    
    /**
     * Check HMIS compliance for income data collection
     */
    @Transactional(readOnly = true)
    public boolean meetsIncomeDataCompliance(UUID enrollmentId) {
        // Must have START record
        if (!hasStartIncomeRecord(enrollmentId)) {
            return false;
        }
        
        // Check if needs annual assessment
        LocalDate oneYearAgo = LocalDate.now().minusYears(1);
        List<UUID> needsAnnual = incomeRepository.findEnrollmentIdsNeedingAnnualAssessment(oneYearAgo);
        if (needsAnnual.contains(enrollmentId)) {
            return false; // Needs annual but doesn't have one
        }
        
        // Check data quality of all records
        List<JpaIncomeBenefitsEntity> records = incomeRepository
            .findByEnrollmentIdOrderByInformationDateDesc(enrollmentId);
        
        return records.stream()
            .map(JpaIncomeBenefitsEntity::toDomainObject)
            .allMatch(IncomeBenefitsRecord::meetsDataQuality);
    }
    
    /**
     * Get income records for HMIS CSV export
     */
    @Transactional(readOnly = true)
    public List<IncomeBenefitsRecord> getIncomeRecordsForHmisExport(
            LocalDate startDate, 
            LocalDate endDate) {
        
        List<JpaIncomeBenefitsEntity> entities = incomeRepository
            .findForHmisExport(startDate, endDate);
        
        return entities.stream()
            .map(JpaIncomeBenefitsEntity::toDomainObject)
            .toList();
    }
    
    /**
     * Get latest income record per enrollment for reporting
     */
    @Transactional(readOnly = true)
    public List<IncomeBenefitsRecord> getLatestIncomeRecordPerEnrollment(
            LocalDate startDate, 
            LocalDate endDate) {
        
        List<JpaIncomeBenefitsEntity> entities = incomeRepository
            .findLatestRecordPerEnrollment(startDate, endDate);
        
        return entities.stream()
            .map(JpaIncomeBenefitsEntity::toDomainObject)
            .toList();
    }
}
