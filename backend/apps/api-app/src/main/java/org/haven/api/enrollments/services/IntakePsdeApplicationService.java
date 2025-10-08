package org.haven.api.enrollments.services;

import org.haven.api.enrollments.dto.IntakePsdeRequest;
import org.haven.api.enrollments.dto.IntakePsdeResponse;
import org.haven.programenrollment.domain.IntakePsdeRecord;
import org.haven.programenrollment.domain.IntakePsdeRepository;
import org.haven.programenrollment.domain.ProgramEnrollment;
import org.haven.programenrollment.domain.ProgramEnrollmentId;
import org.haven.programenrollment.domain.ProgramEnrollmentRepository;
import org.haven.programenrollment.application.validation.IntakePsdeValidationService;
import org.haven.programenrollment.application.services.IntakePsdeAuditLogger;
import org.haven.shared.vo.hmis.DvRedactionFlag;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

/**
 * Application service for Intake PSDE operations at the API layer
 * Coordinates between DTOs, domain logic, validation, and persistence
 */
@Service
public class IntakePsdeApplicationService {

    private final IntakePsdeDtoMapper dtoMapper;
    private final IntakePsdeValidationService validationService;
    private final IntakePsdeAuditLogger auditLogger;
    private final IntakePsdeRepository repository;
    private final ProgramEnrollmentRepository enrollmentRepository;

    public IntakePsdeApplicationService(
            IntakePsdeDtoMapper dtoMapper,
            IntakePsdeValidationService validationService,
            IntakePsdeAuditLogger auditLogger,
            IntakePsdeRepository repository,
            ProgramEnrollmentRepository enrollmentRepository) {
        this.dtoMapper = dtoMapper;
        this.validationService = validationService;
        this.auditLogger = auditLogger;
        this.repository = repository;
        this.enrollmentRepository = enrollmentRepository;
    }

    /**
     * Create new PSDE record from request
     */
    public IntakePsdeResponse createPsdeRecord(
            UUID enrollmentId,
            IntakePsdeRequest request,
            String collectedBy) {

        // Look up enrollment to get client ID
        ProgramEnrollment enrollment = enrollmentRepository.findById(ProgramEnrollmentId.of(enrollmentId))
            .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + enrollmentId));

        UUID clientId = enrollment.getClientId().value();

        // Convert request to domain object
        IntakePsdeRecord record = dtoMapper.requestToRecord(request, enrollmentId, clientId);

        // Validate domain object
        var validationResult = validationService.validateIntakePsdeRecord(record);
        if (validationResult.hasErrors()) {
            throw new IllegalArgumentException("Validation failed: " + validationResult.getErrorSummary());
        }

        // Persist record
        record = repository.save(record);

        // Log audit event
        auditLogger.logRecordCreation(
            record.getRecordId().toString(),
            collectedBy,
            clientId.toString(),
            enrollmentId.toString()
        );

        // Convert to response DTO
        return dtoMapper.recordToResponse(record);
    }

    /**
     * Get all PSDE records for an enrollment
     */
    public List<IntakePsdeResponse> getAllPsdeRecords(String enrollmentId) {
        ProgramEnrollmentId programEnrollmentId = ProgramEnrollmentId.of(UUID.fromString(enrollmentId));
        List<IntakePsdeRecord> records = repository.findActiveByEnrollmentId(programEnrollmentId);
        return records.stream()
            .map(dtoMapper::recordToResponse)
            .toList();
    }

    /**
     * Get PSDE record by enrollment and record ID
     */
    public IntakePsdeResponse getPsdeRecord(String enrollmentId, String recordId) {
        IntakePsdeRecord record = repository.findActiveByRecordId(UUID.fromString(recordId))
            .orElse(null);

        if (record == null) {
            return null;
        }

        // Verify the record belongs to the specified enrollment
        if (!record.getEnrollmentId().value().equals(UUID.fromString(enrollmentId))) {
            return null;
        }

        return dtoMapper.recordToResponse(record);
    }

    /**
     * Get PSDE record by ID
     */
    public IntakePsdeResponse getPsdeRecord(UUID recordId) {
        IntakePsdeRecord record = repository.findActiveByRecordId(recordId)
            .orElse(null);

        if (record == null) {
            return null;
        }

        return dtoMapper.recordToResponse(record);
    }

    /**
     * Update existing PSDE record
     */
    public IntakePsdeResponse updatePsdeRecord(
            UUID recordId,
            IntakePsdeRequest request,
            String updatedBy) {

        // Find existing record
        IntakePsdeRecord existingRecord = repository.findActiveByRecordId(recordId)
            .orElseThrow(() -> new IllegalArgumentException("Record not found: " + recordId));

        // Apply updates to existing record
        dtoMapper.applyUpdatesToRecord(existingRecord, request);

        // Validate updated record
        var validationResult = validationService.validateIntakePsdeRecord(existingRecord);
        if (validationResult.hasErrors()) {
            throw new IllegalArgumentException("Validation failed: " + validationResult.getErrorSummary());
        }

        // Persist updated record
        existingRecord = repository.save(existingRecord);

        // Log audit event
        auditLogger.logRecordUpdate(
            existingRecord.getRecordId().toString(),
            updatedBy,
            "PSDE_UPDATE",
            new String[]{"intakePsdeData"}
        );

        return dtoMapper.recordToResponse(existingRecord);
    }

    /**
     * Get all PSDE records for an enrollment
     */
    public List<IntakePsdeResponse> getPsdeRecordsForEnrollment(UUID enrollmentId) {
        ProgramEnrollmentId programEnrollmentId = ProgramEnrollmentId.of(enrollmentId);
        List<IntakePsdeRecord> records = repository.findActiveByEnrollmentId(programEnrollmentId);
        return records.stream()
            .map(dtoMapper::recordToResponse)
            .toList();
    }

    /**
     * Update VAWA confidentiality settings
     */
    public IntakePsdeResponse updateVawaConfidentiality(
            UUID recordId,
            boolean confidentialityRequested,
            String redactionLevel,
            String reason,
            String updatedBy) {

        // Find existing record
        IntakePsdeRecord record = repository.findActiveByRecordId(recordId)
            .orElseThrow(() -> new IllegalArgumentException("Record not found: " + recordId));

        // Update VAWA confidentiality settings
        DvRedactionFlag redactionFlag = DvRedactionFlag.valueOf(redactionLevel);
        record.updateDomesticViolenceInformation(
            record.getDomesticViolence(),
            record.getDomesticViolenceRecency(),
            record.getCurrentlyFleeingDomesticViolence(),
            redactionFlag,
            confidentialityRequested
        );

        // Persist updated record
        record = repository.save(record);

        // Log audit event
        auditLogger.logVawaConfidentialityChange(
            record.getRecordId().toString(),
            updatedBy,
            !confidentialityRequested, // old value (inverse of new)
            confidentialityRequested,  // new value
            reason
        );

        return dtoMapper.recordToResponse(record);
    }

    /**
     * Get data quality summary for enrollment
     */
    public DataQualitySummary getDataQualitySummary(UUID enrollmentId) {
        ProgramEnrollmentId programEnrollmentId = ProgramEnrollmentId.of(enrollmentId);
        List<IntakePsdeRecord> records = repository.findActiveByEnrollmentId(programEnrollmentId);

        if (records.isEmpty()) {
            return new DataQualitySummary(0, 0, 0, 0, "N/A");
        }

        int totalRecords = records.size();
        long recordsMeetingQuality = records.stream()
            .filter(IntakePsdeRecord::meetsHmisDataQuality)
            .count();
        long recordsWithDvData = records.stream()
            .filter(r -> r.getDomesticViolence() != null)
            .count();
        long highSensitivityDvCases = records.stream()
            .filter(IntakePsdeRecord::isHighSensitivityDvCase)
            .count();

        String qualityPercentage = String.format("%.1f%%",
            (recordsMeetingQuality * 100.0) / totalRecords);

        return new DataQualitySummary(
            totalRecords,
            (int) recordsMeetingQuality,
            (int) recordsWithDvData,
            (int) highSensitivityDvCases,
            qualityPercentage
        );
    }

    /**
     * Data quality summary record
     */
    public record DataQualitySummary(
        int totalRecords,
        int recordsMeetingHmisQuality,
        int recordsWithDvData,
        int highSensitivityDvCases,
        String dataQualityPercentage
    ) {}
}