package org.haven.api.reporting;

import org.haven.api.config.SecurityUtils;
import org.haven.reporting.application.services.*;
import org.haven.reporting.domain.pithic.*;
import org.haven.shared.vo.UserId;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;

import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.io.OutputStream;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * REST API endpoints for PIT/HIC report generation and delivery.
 * Implements role-based access control and audit logging.
 */
@RestController
@RequestMapping("/api/v1/reports/pit-hic")
@Tag(name = "PIT/HIC Reports", description = "Point-in-Time and Housing Inventory Count reporting endpoints")
@SecurityRequirement(name = "bearerAuth")
public class PitHicReportController {

    private static final Logger log = LoggerFactory.getLogger(PitHicReportController.class);

    private final PitHicAggregationService aggregationService;
    private final PitHicEtlJobService etlJobService;
    private final PitHicValidationService validationService;
    private final PitHicExportService exportService;
    private final PitHicAuditService auditService;
    private final SecurityUtils securityUtils;

    public PitHicReportController(
            PitHicAggregationService aggregationService,
            PitHicEtlJobService etlJobService,
            PitHicValidationService validationService,
            PitHicExportService exportService,
            PitHicAuditService auditService,
            SecurityUtils securityUtils) {
        this.aggregationService = aggregationService;
        this.etlJobService = etlJobService;
        this.validationService = validationService;
        this.exportService = exportService;
        this.auditService = auditService;
        this.securityUtils = securityUtils;
    }

    /**
     * Generate PIT census report for a specific date and continuum.
     */
    @PostMapping("/pit/generate")
    @PreAuthorize("hasAnyRole('ADMIN', 'REPORT_MANAGER', 'HUD_REPORTER')")
    @Operation(summary = "Generate PIT census report",
               description = "Generates a Point-in-Time census report for the specified date and continuum")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Report generated successfully"),
        @ApiResponse(responseCode = "403", description = "Insufficient permissions"),
        @ApiResponse(responseCode = "400", description = "Invalid parameters")
    })
    public ResponseEntity<PitCensusReportDto> generatePitCensus(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @Parameter(description = "Census date", required = true)
            LocalDate censusDate,

            @RequestParam
            @Parameter(description = "Continuum code", required = true)
            String continuumCode,

            @RequestParam
            @Parameter(description = "Organization ID", required = true)
            String organizationId,

            @RequestParam(required = false)
            @Parameter(description = "Purpose of report generation")
            String purpose) {

        UserId userId = securityUtils.getCurrentUserId();

        log.info("User {} generating PIT census for date: {}, continuum: {}, org: {}",
                userId.getValue(), censusDate, continuumCode, organizationId);

        // Audit the report generation request
        auditService.auditReportGeneration(
            "PIT_CENSUS",
            censusDate,
            continuumCode,
            organizationId,
            userId,
            purpose
        );

        // Generate the census data
        PitCensusData censusData = aggregationService.generatePitCensusData(
            censusDate, continuumCode, organizationId, userId
        );

        // Validate the generated data
        PitHicValidationService.PitValidationResult validation =
            validationService.validatePitCensus(censusData.getCensusId());

        // Convert to DTO
        PitCensusReportDto dto = PitCensusReportDto.fromDomain(censusData, validation);

        return ResponseEntity.ok(dto);
    }

    /**
     * Generate HIC inventory report for a specific date and continuum.
     */
    @PostMapping("/hic/generate")
    @PreAuthorize("hasAnyRole('ADMIN', 'REPORT_MANAGER', 'HUD_REPORTER')")
    @Operation(summary = "Generate HIC inventory report",
               description = "Generates a Housing Inventory Count report for the specified date and continuum")
    public ResponseEntity<HicInventoryReportDto> generateHicInventory(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
            @Parameter(description = "Inventory date", required = true)
            LocalDate inventoryDate,

            @RequestParam
            @Parameter(description = "Continuum code", required = true)
            String continuumCode,

            @RequestParam
            @Parameter(description = "Organization ID", required = true)
            String organizationId,

            @RequestParam(required = false)
            @Parameter(description = "Purpose of report generation")
            String purpose) {

        UserId userId = securityUtils.getCurrentUserId();

        log.info("User {} generating HIC inventory for date: {}, continuum: {}, org: {}",
                userId.getValue(), inventoryDate, continuumCode, organizationId);

        // Audit the report generation request
        auditService.auditReportGeneration(
            "HIC_INVENTORY",
            inventoryDate,
            continuumCode,
            organizationId,
            userId,
            purpose
        );

        // Generate the inventory data
        HicInventoryData inventoryData = aggregationService.generateHicInventoryData(
            inventoryDate, continuumCode, organizationId, userId
        );

        // Validate the generated data
        PitHicValidationService.HicValidationResult validation =
            validationService.validateHicInventory(inventoryData.getInventoryId());

        // Convert to DTO
        HicInventoryReportDto dto = HicInventoryReportDto.fromDomain(inventoryData, validation);

        return ResponseEntity.ok(dto);
    }

    /**
     * Export PIT census report in specified format.
     */
    @GetMapping("/pit/{censusId}/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'REPORT_MANAGER', 'HUD_REPORTER', 'REPORT_VIEWER')")
    @Operation(summary = "Export PIT census report",
               description = "Exports a PIT census report in the specified format (CSV, PDF, JSON)")
    public void exportPitCensus(
            @PathVariable UUID censusId,

            @RequestParam(defaultValue = "CSV")
            @Parameter(description = "Export format", example = "CSV")
            ExportFormat format,

            @RequestParam(required = false)
            @Parameter(description = "Purpose of export")
            String purpose,

            HttpServletResponse response) throws IOException {

        UserId userId = securityUtils.getCurrentUserId();

        log.info("User {} exporting PIT census {} as {}",
                userId.getValue(), censusId, format);

        // Audit the export
        auditService.auditReportExport(
            "PIT_CENSUS",
            censusId,
            format.toString(),
            userId,
            purpose
        );

        // Get the census data
        PitCensusData censusData = exportService.getPitCensusData(censusId);

        // Set response headers
        setExportHeaders(response, format, "pit_census_" + censusData.getCensusDate());

        // Export based on format
        try (OutputStream outputStream = response.getOutputStream()) {
            switch (format) {
                case CSV:
                    exportService.exportPitCensusCsv(censusData, outputStream);
                    break;
                case PDF:
                    exportService.exportPitCensusPdf(censusData, outputStream);
                    break;
                case JSON:
                    exportService.exportPitCensusJson(censusData, outputStream);
                    break;
                case XML:
                    exportService.exportPitCensusXml(censusData, outputStream);
                    break;
            }
        }
    }

    /**
     * Export HIC inventory report in specified format.
     */
    @GetMapping("/hic/{inventoryId}/export")
    @PreAuthorize("hasAnyRole('ADMIN', 'REPORT_MANAGER', 'HUD_REPORTER', 'REPORT_VIEWER')")
    @Operation(summary = "Export HIC inventory report",
               description = "Exports a HIC inventory report in the specified format (CSV, PDF, JSON)")
    public void exportHicInventory(
            @PathVariable UUID inventoryId,

            @RequestParam(defaultValue = "CSV")
            @Parameter(description = "Export format", example = "CSV")
            ExportFormat format,

            @RequestParam(required = false)
            @Parameter(description = "Purpose of export")
            String purpose,

            HttpServletResponse response) throws IOException {

        UserId userId = securityUtils.getCurrentUserId();

        log.info("User {} exporting HIC inventory {} as {}",
                userId.getValue(), inventoryId, format);

        // Audit the export
        auditService.auditReportExport(
            "HIC_INVENTORY",
            inventoryId,
            format.toString(),
            userId,
            purpose
        );

        // Get the inventory data
        HicInventoryData inventoryData = exportService.getHicInventoryData(inventoryId);

        // Set response headers
        setExportHeaders(response, format, "hic_inventory_" + inventoryData.getInventoryDate());

        // Export based on format
        try (OutputStream outputStream = response.getOutputStream()) {
            switch (format) {
                case CSV:
                    exportService.exportHicInventoryCsv(inventoryData, outputStream);
                    break;
                case PDF:
                    exportService.exportHicInventoryPdf(inventoryData, outputStream);
                    break;
                case JSON:
                    exportService.exportHicInventoryJson(inventoryData, outputStream);
                    break;
                case XML:
                    exportService.exportHicInventoryXml(inventoryData, outputStream);
                    break;
            }
        }
    }

    /**
     * Get combined PIT/HIC annual report.
     */
    @GetMapping("/annual/{year}/{continuumCode}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REPORT_MANAGER', 'HUD_REPORTER')")
    @Operation(summary = "Get annual PIT/HIC report",
               description = "Retrieves the combined annual PIT/HIC report for a continuum")
    public ResponseEntity<AnnualPitHicReportDto> getAnnualReport(
            @PathVariable int year,
            @PathVariable String continuumCode,
            @RequestParam(required = false) String organizationId) {

        UserId userId = securityUtils.getCurrentUserId();

        log.info("User {} retrieving annual PIT/HIC report for year: {}, continuum: {}",
                userId.getValue(), year, continuumCode);

        AnnualPitHicReport report = exportService.getAnnualReport(
            year, continuumCode, organizationId
        );

        if (report == null) {
            return ResponseEntity.notFound().build();
        }

        AnnualPitHicReportDto dto = AnnualPitHicReportDto.fromDomain(report);
        return ResponseEntity.ok(dto);
    }

    /**
     * Start ETL job for PIT/HIC data aggregation.
     */
    @PostMapping("/etl/start")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "Start PIT/HIC ETL job",
               description = "Manually triggers an ETL job for PIT/HIC data aggregation")
    public ResponseEntity<EtlJobStatusDto> startEtlJob(
            @RequestBody EtlJobRequest request) {

        UserId userId = securityUtils.getCurrentUserId();

        log.info("User {} starting ETL job for date: {}, type: {}",
                userId.getValue(), request.processDate(), request.jobType());

        UUID jobId;
        if (request.jobType() == JobType.PIT_CENSUS) {
            jobId = etlJobService.startPitCensusJob(
                request.processDate(),
                "MANUAL",
                userId
            );
        } else {
            jobId = etlJobService.startHicInventoryJob(
                request.processDate(),
                "MANUAL",
                userId
            );
        }

        PitHicEtlJobService.JobStatus status = etlJobService.getJobStatus(jobId);
        EtlJobStatusDto dto = EtlJobStatusDto.fromJobStatus(status);

        return ResponseEntity.ok(dto);
    }

    /**
     * Get ETL job status.
     */
    @GetMapping("/etl/status/{jobId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'REPORT_MANAGER')")
    @Operation(summary = "Get ETL job status",
               description = "Retrieves the current status of an ETL job")
    public ResponseEntity<EtlJobStatusDto> getEtlJobStatus(@PathVariable UUID jobId) {

        PitHicEtlJobService.JobStatus status = etlJobService.getJobStatus(jobId);

        if (status == null) {
            return ResponseEntity.notFound().build();
        }

        EtlJobStatusDto dto = EtlJobStatusDto.fromJobStatus(status);
        return ResponseEntity.ok(dto);
    }

    /**
     * Validate PIT census data.
     */
    @PostMapping("/pit/{censusId}/validate")
    @PreAuthorize("hasAnyRole('ADMIN', 'REPORT_MANAGER', 'HUD_REPORTER')")
    @Operation(summary = "Validate PIT census",
               description = "Validates PIT census data against HUD requirements")
    public ResponseEntity<ValidationResultDto> validatePitCensus(@PathVariable UUID censusId) {

        log.info("Validating PIT census: {}", censusId);

        PitHicValidationService.PitValidationResult result =
            validationService.validatePitCensus(censusId);

        ValidationResultDto dto = ValidationResultDto.fromPitValidation(result);
        return ResponseEntity.ok(dto);
    }

    /**
     * Validate HIC inventory data.
     */
    @PostMapping("/hic/{inventoryId}/validate")
    @PreAuthorize("hasAnyRole('ADMIN', 'REPORT_MANAGER', 'HUD_REPORTER')")
    @Operation(summary = "Validate HIC inventory",
               description = "Validates HIC inventory data against HUD requirements")
    public ResponseEntity<ValidationResultDto> validateHicInventory(@PathVariable UUID inventoryId) {

        log.info("Validating HIC inventory: {}", inventoryId);

        PitHicValidationService.HicValidationResult result =
            validationService.validateHicInventory(inventoryId);

        ValidationResultDto dto = ValidationResultDto.fromHicValidation(result);
        return ResponseEntity.ok(dto);
    }

    /**
     * Cross-validate PIT and HIC data.
     */
    @PostMapping("/cross-validate")
    @PreAuthorize("hasAnyRole('ADMIN', 'REPORT_MANAGER', 'HUD_REPORTER')")
    @Operation(summary = "Cross-validate PIT and HIC",
               description = "Performs cross-validation between PIT census and HIC inventory data")
    public ResponseEntity<ValidationResultDto> crossValidate(
            @RequestParam UUID censusId,
            @RequestParam UUID inventoryId) {

        log.info("Cross-validating PIT {} and HIC {}", censusId, inventoryId);

        PitHicValidationService.CrossValidationResult result =
            validationService.crossValidatePitHic(censusId, inventoryId);

        ValidationResultDto dto = ValidationResultDto.fromCrossValidation(result);
        return ResponseEntity.ok(dto);
    }

    /**
     * Get available reports for a date range.
     */
    @GetMapping("/available")
    @PreAuthorize("hasAnyRole('ADMIN', 'REPORT_MANAGER', 'HUD_REPORTER', 'REPORT_VIEWER')")
    @Operation(summary = "List available reports",
               description = "Lists all available PIT/HIC reports within a date range")
    public ResponseEntity<List<ReportSummaryDto>> getAvailableReports(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) String continuumCode,
            @RequestParam(required = false) String organizationId) {

        List<PitHicExportService.ReportSummary> reports = exportService.getAvailableReports(
            startDate, endDate, continuumCode, organizationId
        );

        List<ReportSummaryDto> dtos = reports.stream()
            .map(ReportSummaryDto::fromDomain)
            .collect(Collectors.toList());

        return ResponseEntity.ok(dtos);
    }

    // Helper methods

    private void setExportHeaders(HttpServletResponse response, ExportFormat format, String filename) {
        switch (format) {
            case CSV:
                response.setContentType("text/csv");
                response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + filename + ".csv\"");
                break;
            case PDF:
                response.setContentType(MediaType.APPLICATION_PDF_VALUE);
                response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + filename + ".pdf\"");
                break;
            case JSON:
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + filename + ".json\"");
                break;
            case XML:
                response.setContentType(MediaType.APPLICATION_XML_VALUE);
                response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                    "attachment; filename=\"" + filename + ".xml\"");
                break;
        }
    }

    // Enums

    public enum ExportFormat {
        CSV,
        PDF,
        JSON,
        XML
    }

    public enum JobType {
        PIT_CENSUS,
        HIC_INVENTORY
    }

    // DTOs

    public record EtlJobRequest(
        LocalDate processDate,
        JobType jobType,
        List<String> continuumCodes,
        List<String> organizationIds
    ) {}

    public static class PitCensusReportDto {
        private UUID censusId;
        private LocalDate censusDate;
        private String continuumCode;
        private Map<String, Integer> householdCounts;
        private Map<String, Integer> demographicCounts;
        private Map<String, Integer> locationCounts;
        private ValidationStatus validationStatus;
        private List<ValidationIssue> validationIssues;

        public static PitCensusReportDto fromDomain(
                PitCensusData data,
                PitHicValidationService.PitValidationResult validation) {
            // Implementation
            return new PitCensusReportDto();
        }

        // Getters and setters
    }

    public static class HicInventoryReportDto {
        private UUID inventoryId;
        private LocalDate inventoryDate;
        private String continuumCode;
        private Map<String, Integer> bedCounts;
        private Map<String, Integer> unitCounts;
        private Map<String, Double> utilizationRates;
        private ValidationStatus validationStatus;
        private List<ValidationIssue> validationIssues;

        public static HicInventoryReportDto fromDomain(
                HicInventoryData data,
                PitHicValidationService.HicValidationResult validation) {
            // Implementation
            return new HicInventoryReportDto();
        }

        // Getters and setters
    }

    public static class AnnualPitHicReportDto {
        private int year;
        private String continuumCode;
        private PitCensusReportDto pitReport;
        private HicInventoryReportDto hicReport;
        private Map<String, Object> combinedMetrics;

        public static AnnualPitHicReportDto fromDomain(AnnualPitHicReport report) {
            // Implementation
            return new AnnualPitHicReportDto();
        }

        // Getters and setters
    }

    public static class EtlJobStatusDto {
        private UUID jobId;
        private JobType type;
        private LocalDate processDate;
        private String state;
        private int processedCount;
        private int errorCount;

        public static EtlJobStatusDto fromJobStatus(PitHicEtlJobService.JobStatus status) {
            // Implementation
            return new EtlJobStatusDto();
        }

        // Getters and setters
    }

    public static class ValidationResultDto {
        private ValidationStatus status;
        private List<ValidationIssue> issues;
        private Map<String, Object> metrics;

        public static ValidationResultDto fromPitValidation(
                PitHicValidationService.PitValidationResult result) {
            // Implementation
            return new ValidationResultDto();
        }

        public static ValidationResultDto fromHicValidation(
                PitHicValidationService.HicValidationResult result) {
            // Implementation
            return new ValidationResultDto();
        }

        public static ValidationResultDto fromCrossValidation(
                PitHicValidationService.CrossValidationResult result) {
            // Implementation
            return new ValidationResultDto();
        }

        // Getters and setters
    }

    public static class ReportSummaryDto {
        private UUID reportId;
        private String reportType;
        private LocalDate reportDate;
        private String continuumCode;
        private String organizationId;
        private ValidationStatus status;

        public static ReportSummaryDto fromDomain(PitHicExportService.ReportSummary summary) {
            // Implementation
            return new ReportSummaryDto();
        }

        // Getters and setters
    }

    public enum ValidationStatus {
        PASSED,
        PASSED_WITH_WARNINGS,
        FAILED
    }

    public static class ValidationIssue {
        private String severity;
        private String code;
        private String description;

        // Getters and setters
    }
}

