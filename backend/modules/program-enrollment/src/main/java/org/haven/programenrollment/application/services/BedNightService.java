package org.haven.programenrollment.application.services;

import org.haven.programenrollment.domain.*;
import org.haven.programenrollment.infrastructure.persistence.*;
import org.haven.clientprofile.domain.ClientId;
import org.springframework.stereotype.Service;
import org.springframework.context.annotation.Lazy;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.UUID;

/**
 * Application Service for HMIS-compliant Bed Night management
 * Handles Emergency Shelter Night-by-Night (ES-NbN) bed tracking
 * Critical for bed utilization reporting and client service tracking
 */
@Service
@Transactional
public class BedNightService {
    
    private final ProgramEnrollmentRepository enrollmentRepository;
    private final JpaBedNightRepository bedNightRepository;
    
    public BedNightService(
            @Lazy ProgramEnrollmentRepository enrollmentRepository,
            JpaBedNightRepository bedNightRepository) {
        this.enrollmentRepository = enrollmentRepository;
        this.bedNightRepository = bedNightRepository;
    }
    
    /**
     * Record single bed night for enrollment
     */
    public BedNight recordBedNight(
            UUID enrollmentId,
            LocalDate bedNightDate,
            String createdBy) {
        
        // Load enrollment aggregate
        ProgramEnrollmentId domainId = ProgramEnrollmentId.of(enrollmentId);
        ProgramEnrollment enrollment = enrollmentRepository.findById(domainId)
            .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + enrollmentId));
        
        // Record bed night through aggregate
        enrollment.addBedNight(bedNightDate, createdBy);
        
        // Get the created record
        BedNight bedNightRecord = enrollment.getBedNight(bedNightDate);
        
        // Persist the record
        JpaBedNightEntity bedNightEntity = new JpaBedNightEntity(bedNightRecord);
        bedNightRepository.save(bedNightEntity);
        
        // Update enrollment
        enrollmentRepository.save(enrollment);
        
        return bedNightRecord;
    }
    
    /**
     * Record range of consecutive bed nights
     */
    public List<BedNight> recordBedNightRange(
            UUID enrollmentId,
            LocalDate startDate,
            LocalDate endDate,
            String createdBy) {
        
        // Load enrollment aggregate
        ProgramEnrollmentId domainId = ProgramEnrollmentId.of(enrollmentId);
        ProgramEnrollment enrollment = enrollmentRepository.findById(domainId)
            .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + enrollmentId));
        
        // Record bed night range through aggregate
        enrollment.addBedNightRange(startDate, endDate, createdBy);
        
        // Get the created records
        List<BedNight> bedNightRecords = new java.util.ArrayList<>();
        LocalDate currentDate = startDate;
        while (!currentDate.isAfter(endDate)) {
            BedNight bedNight = enrollment.getBedNight(currentDate);
            if (bedNight != null) {
                bedNightRecords.add(bedNight);
                
                // Persist the record
                JpaBedNightEntity bedNightEntity = new JpaBedNightEntity(bedNight);
                bedNightRepository.save(bedNightEntity);
            }
            currentDate = currentDate.plusDays(1);
        }
        
        // Update enrollment
        enrollmentRepository.save(enrollment);
        
        return bedNightRecords;
    }
    
    /**
     * Remove bed night record
     */
    public void removeBedNight(UUID enrollmentId, LocalDate bedNightDate) {
        // Load enrollment aggregate
        ProgramEnrollmentId domainId = ProgramEnrollmentId.of(enrollmentId);
        ProgramEnrollment enrollment = enrollmentRepository.findById(domainId)
            .orElseThrow(() -> new IllegalArgumentException("Enrollment not found: " + enrollmentId));
        
        // Get the bed night to remove
        BedNight bedNightToRemove = enrollment.getBedNight(bedNightDate);
        if (bedNightToRemove == null) {
            throw new IllegalArgumentException("Bed night not found for date: " + bedNightDate);
        }
        
        // Remove from aggregate
        enrollment.removeBedNight(bedNightDate);
        
        // Delete from repository
        bedNightRepository.deleteByEnrollmentIdAndBedNightDate(enrollmentId, bedNightDate);
        
        // Update enrollment
        enrollmentRepository.save(enrollment);
    }
    
    /**
     * Get all bed nights for an enrollment
     */
    @Transactional(readOnly = true)
    public List<BedNight> getBedNightsForEnrollment(UUID enrollmentId) {
        List<JpaBedNightEntity> entities = bedNightRepository
            .findByEnrollmentIdOrderByBedNightDateDesc(enrollmentId);
        
        return entities.stream()
            .map(JpaBedNightEntity::toDomainObject)
            .toList();
    }
    
    /**
     * Get bed nights for enrollment within date range
     */
    @Transactional(readOnly = true)
    public List<BedNight> getBedNightsForEnrollmentInDateRange(
            UUID enrollmentId,
            LocalDate startDate,
            LocalDate endDate) {
        
        List<JpaBedNightEntity> entities = bedNightRepository
            .findByEnrollmentIdAndBedNightDateBetween(enrollmentId, startDate, endDate);
        
        return entities.stream()
            .map(JpaBedNightEntity::toDomainObject)
            .toList();
    }
    
    /**
     * Get all bed nights for a client across enrollments
     */
    @Transactional(readOnly = true)
    public List<BedNight> getBedNightsForClient(UUID clientId) {
        List<JpaBedNightEntity> entities = bedNightRepository
            .findByClientIdOrderByBedNightDateDesc(clientId);
        
        return entities.stream()
            .map(JpaBedNightEntity::toDomainObject)
            .toList();
    }
    
    /**
     * Get bed nights within date range (all enrollments)
     */
    @Transactional(readOnly = true)
    public List<BedNight> getBedNightsInDateRange(
            LocalDate startDate, 
            LocalDate endDate) {
        
        List<JpaBedNightEntity> entities = bedNightRepository
            .findByBedNightDateBetween(startDate, endDate);
        
        return entities.stream()
            .map(JpaBedNightEntity::toDomainObject)
            .toList();
    }
    
    /**
     * Check if bed night exists for enrollment and date
     */
    @Transactional(readOnly = true)
    public boolean hasBedNight(UUID enrollmentId, LocalDate bedNightDate) {
        return bedNightRepository.existsByEnrollmentIdAndBedNightDate(enrollmentId, bedNightDate);
    }
    
    /**
     * Get bed night for specific enrollment and date
     */
    @Transactional(readOnly = true)
    public BedNight getBedNight(UUID enrollmentId, LocalDate bedNightDate) {
        return bedNightRepository.findByEnrollmentIdAndBedNightDate(enrollmentId, bedNightDate)
            .map(JpaBedNightEntity::toDomainObject)
            .orElse(null);
    }
    
    /**
     * Count total bed nights for enrollment
     */
    @Transactional(readOnly = true)
    public long countBedNightsForEnrollment(UUID enrollmentId) {
        return bedNightRepository.countByEnrollmentId(enrollmentId);
    }
    
    /**
     * Count bed nights within date range
     */
    @Transactional(readOnly = true)
    public long countBedNightsInDateRange(LocalDate startDate, LocalDate endDate) {
        return bedNightRepository.countByBedNightDateBetween(startDate, endDate);
    }
    
    /**
     * Calculate bed utilization metrics
     */
    @Transactional(readOnly = true)
    public BedUtilizationMetrics calculateBedUtilizationMetrics(
            LocalDate startDate,
            LocalDate endDate) {
        
        long totalBedNights = bedNightRepository.countByBedNightDateBetween(startDate, endDate);
        
        // Calculate unique clients served
        List<JpaBedNightEntity> bedNights = bedNightRepository
            .findByBedNightDateBetween(startDate, endDate);
        
        int uniqueClients = (int) bedNights.stream()
            .map(entity -> entity.toDomainObject().getClientId().value())
            .distinct()
            .count();
        
        // Calculate unique enrollments
        int uniqueEnrollments = (int) bedNights.stream()
            .map(entity -> entity.toDomainObject().getEnrollmentId().value())
            .distinct()
            .count();
        
        long totalDays = ChronoUnit.DAYS.between(startDate, endDate) + 1;
        double averageDailyUtilization = totalDays > 0 ? (double) totalBedNights / totalDays : 0.0;
        
        return new BedUtilizationMetrics(
            totalBedNights,
            uniqueClients,
            uniqueEnrollments,
            averageDailyUtilization,
            startDate,
            endDate
        );
    }
    
    /**
     * Get consecutive nights analysis for enrollment
     */
    @Transactional(readOnly = true)
    public ConsecutiveNightsAnalysis getConsecutiveNightsAnalysis(UUID enrollmentId) {
        List<JpaBedNightEntity> bedNights = bedNightRepository
            .findByEnrollmentIdOrderByBedNightDateDesc(enrollmentId);
        
        if (bedNights.isEmpty()) {
            return new ConsecutiveNightsAnalysis(0, 0, null, null, 0);
        }
        
        // Sort by date ascending for analysis
        List<LocalDate> bedNightDates = bedNights.stream()
            .map(entity -> entity.toDomainObject().getBedNightDate())
            .sorted()
            .toList();
        
        int totalNights = bedNightDates.size();
        int longestStreak = 0;
        int currentStreak = 1;
        LocalDate longestStreakStart = bedNightDates.get(0);
        LocalDate longestStreakEnd = bedNightDates.get(0);
        LocalDate currentStreakStart = bedNightDates.get(0);
        
        for (int i = 1; i < bedNightDates.size(); i++) {
            LocalDate currentDate = bedNightDates.get(i);
            LocalDate previousDate = bedNightDates.get(i - 1);
            
            if (currentDate.equals(previousDate.plusDays(1))) {
                // Consecutive night
                currentStreak++;
            } else {
                // Streak broken
                if (currentStreak > longestStreak) {
                    longestStreak = currentStreak;
                    longestStreakStart = currentStreakStart;
                    longestStreakEnd = bedNightDates.get(i - 1);
                }
                currentStreak = 1;
                currentStreakStart = currentDate;
            }
        }
        
        // Check final streak
        if (currentStreak > longestStreak) {
            longestStreak = currentStreak;
            longestStreakStart = currentStreakStart;
            longestStreakEnd = bedNightDates.get(bedNightDates.size() - 1);
        }
        
        // Calculate gaps (nights between bed nights)
        int totalGaps = 0;
        for (int i = 1; i < bedNightDates.size(); i++) {
            LocalDate currentDate = bedNightDates.get(i);
            LocalDate previousDate = bedNightDates.get(i - 1);
            long daysBetween = ChronoUnit.DAYS.between(previousDate, currentDate) - 1;
            if (daysBetween > 0) {
                totalGaps += daysBetween;
            }
        }
        
        return new ConsecutiveNightsAnalysis(
            totalNights,
            longestStreak,
            longestStreakStart,
            longestStreakEnd,
            totalGaps
        );
    }
    
    /**
     * Get bed night patterns for enrollment (identify regular vs sporadic use)
     */
    @Transactional(readOnly = true)
    public BedNightPattern getBedNightPattern(UUID enrollmentId) {
        ConsecutiveNightsAnalysis analysis = getConsecutiveNightsAnalysis(enrollmentId);
        
        if (analysis.totalNights() == 0) {
            return BedNightPattern.NO_USAGE;
        }
        
        double continuityRatio = analysis.totalNights() > 0 ? 
            (double) analysis.longestStreak() / analysis.totalNights() : 0.0;
        
        if (continuityRatio >= 0.8) {
            return BedNightPattern.CONSISTENT_NIGHTLY;
        } else if (continuityRatio >= 0.5) {
            return BedNightPattern.MOSTLY_REGULAR;
        } else if (analysis.totalGaps() > analysis.totalNights()) {
            return BedNightPattern.SPORADIC_WITH_GAPS;
        } else {
            return BedNightPattern.INTERMITTENT;
        }
    }
    
    /**
     * Find enrollments with gaps in bed night tracking
     */
    @Transactional(readOnly = true)
    public List<UUID> findEnrollmentsWithBedNightGaps(int minGapDays) {
        // Use a broad date range to get recent enrollments
        LocalDate yearAgo = LocalDate.now().minusYears(1);
        LocalDate now = LocalDate.now();
        
        return enrollmentRepository.findByEnrollmentDateBetween(yearAgo, now).stream()
            .filter(enrollment -> !enrollment.hasExited())
            .filter(enrollment -> {
                ConsecutiveNightsAnalysis analysis = getConsecutiveNightsAnalysis(enrollment.getId().value());
                return analysis.totalGaps() >= minGapDays;
            })
            .map(enrollment -> enrollment.getId().value())
            .toList();
    }
    
    /**
     * Bulk record bed nights for multiple enrollments (useful for data imports)
     */
    public List<BedNight> recordBulkBedNights(List<BedNightRequest> bedNightRequests) {
        List<BedNight> createdRecords = new java.util.ArrayList<>();
        
        for (BedNightRequest request : bedNightRequests) {
            try {
                BedNight record = recordBedNight(
                    request.enrollmentId(),
                    request.bedNightDate(),
                    request.createdBy()
                );
                createdRecords.add(record);
            } catch (Exception e) {
                // Log error but continue with other records
                // Could be enhanced with detailed error reporting
                System.err.println("Failed to create bed night for " + 
                    request.enrollmentId() + " on " + request.bedNightDate() + ": " + e.getMessage());
            }
        }
        
        return createdRecords;
    }
    
    /**
     * Get bed availability report (requires bed capacity configuration)
     */
    @Transactional(readOnly = true)
    public BedAvailabilityReport getBedAvailabilityReport(LocalDate reportDate, int totalBedCapacity) {
        long bedsUsed = bedNightRepository.countByBedNightDateBetween(reportDate, reportDate);
        long bedsAvailable = Math.max(0, totalBedCapacity - bedsUsed);
        double occupancyRate = totalBedCapacity > 0 ? (double) bedsUsed / totalBedCapacity : 0.0;
        
        return new BedAvailabilityReport(
            reportDate,
            totalBedCapacity,
            bedsUsed,
            bedsAvailable,
            occupancyRate
        );
    }
    
    /**
     * Value object for bed utilization metrics
     */
    public record BedUtilizationMetrics(
        long totalBedNights,
        int uniqueClients,
        int uniqueEnrollments,
        double averageDailyUtilization,
        LocalDate startDate,
        LocalDate endDate
    ) {}
    
    /**
     * Value object for consecutive nights analysis
     */
    public record ConsecutiveNightsAnalysis(
        int totalNights,
        int longestStreak,
        LocalDate longestStreakStart,
        LocalDate longestStreakEnd,
        int totalGaps
    ) {}
    
    /**
     * Enumeration for bed night usage patterns
     */
    public enum BedNightPattern {
        NO_USAGE,
        CONSISTENT_NIGHTLY,
        MOSTLY_REGULAR,
        INTERMITTENT,
        SPORADIC_WITH_GAPS
    }
    
    /**
     * Value object for bed night requests
     */
    public record BedNightRequest(
        UUID enrollmentId,
        LocalDate bedNightDate,
        String createdBy
    ) {}
    
    /**
     * Value object for bed availability report
     */
    public record BedAvailabilityReport(
        LocalDate reportDate,
        long totalBedCapacity,
        long bedsUsed,
        long bedsAvailable,
        double occupancyRate
    ) {}
}
