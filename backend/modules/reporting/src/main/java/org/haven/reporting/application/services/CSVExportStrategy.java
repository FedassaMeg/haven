package org.haven.reporting.application.services;

import org.haven.reporting.application.validation.*;
import org.haven.reporting.domain.ExportPeriod;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * CSV export strategy following RFC 4180 and HUD HMIS CSV specifications:
 * - UTF-8 with BOM
 * - CRLF line endings
 * - HUD-mandated column ordering
 * - Quoted fields containing special characters
 */
@Component
public class CSVExportStrategy implements HUDExportFormatter.FormatStrategy {

    private static final Logger logger = LoggerFactory.getLogger(CSVExportStrategy.class);

    private static final byte[] UTF8_BOM = {(byte) 0xEF, (byte) 0xBB, (byte) 0xBF};
    private static final String LINE_SEPARATOR = "\r\n";
    private static final String FIELD_SEPARATOR = ",";

    // HUD-mandated column order for common sections
    private static final Map<String, List<String>> HUD_COLUMN_ORDER = Map.of(
            "Export", List.of("ExportID", "ExportDate", "ExportStartDate", "ExportEndDate", "SourceSystemName", "SourceSystemID", "SourceContactEmail", "SourceContactPhone", "ExportPeriodType", "PeriodType", "HashStatus"),
            "Organization", List.of("OrganizationID", "OrganizationName", "VictimServiceProvider", "OrganizationCommonName", "DateCreated", "DateUpdated", "UserID", "DateDeleted", "ExportID"),
            "Project", List.of("ProjectID", "OrganizationID", "ProjectName", "ProjectCommonName", "ContinuumProject", "ProjectType", "ResidentialAffiliation", "TrackingMethod", "TargetPopulation", "PITCount", "DateCreated", "DateUpdated", "UserID", "DateDeleted", "ExportID"),
            "Enrollment", List.of("EnrollmentID", "PersonalID", "ProjectID", "EntryDate", "HouseholdID", "RelationshipToHoH", "EnrollmentCoC", "LivingSituation", "LOSUnderThreshold", "PreviousStreetESSH", "DateToStreetESSH", "TimesHomelessPastThreeYears", "MonthsHomelessPastThreeYears", "DisablingCondition", "DateOfEngagement", "MoveInDate", "DateOfPATHStatus", "ClientEnrolledInPATH", "ReasonNotEnrolled", "DateCreated", "DateUpdated", "UserID", "DateDeleted", "ExportID"),
            "Exit", List.of("ExitID", "EnrollmentID", "PersonalID", "ExitDate", "Destination", "OtherDestination", "HousingAssessment", "SubsidyInformation", "ProjectCompletionStatus", "EarlyExitReason", "ExchangeForSex", "ExchangeForSexPastThreeMonths", "CountOfExchangeForSex", "AskedOrForcedToExchangeForSex", "AskedOrForcedToExchangeForSexPastThreeMonths", "WorkPlaceViolenceThreats", "WorkplacePromiseDifference", "CoercedToContinueWork", "LaborExploitPastThreeMonths", "CounselingReceived", "IndividualCounseling", "FamilyCounseling", "GroupCounseling", "SessionCountAtExit", "PostExitCounselingPlan", "SessionsInPlan", "DestinationSafeClient", "DestinationSafeWorker", "PosAdultConnections", "PosPeerConnections", "PosCommunityConnections", "AftercareDate", "AftercareProvided", "EmailSocialMedia", "Telephone", "InPersonIndividual", "InPersonGroup", "CMExitReason", "DateCreated", "DateUpdated", "UserID", "DateDeleted", "ExportID"),
            "Client", List.of("PersonalID", "FirstName", "MiddleName", "LastName", "NameSuffix", "NameDataQuality", "SSN", "SSNDataQuality", "DOB", "DOBDataQuality", "AmIndAKNative", "Asian", "BlackAfAmerican", "NativeHIPacific", "White", "RaceNone", "AdditionalRaceEthnicity", "Woman", "Man", "NonBinary", "CulturallySpecific", "Transgender", "Questioning", "DifferentIdentity", "GenderNone", "DifferentIdentityText", "DateCreated", "DateUpdated", "UserID", "DateDeleted", "ExportID"),
            "Services", List.of("ServicesID", "EnrollmentID", "PersonalID", "DateProvided", "RecordType", "TypeProvided", "OtherTypeProvided", "SubTypeProvided", "FAAmount", "ReferralOutcome", "DateCreated", "DateUpdated", "UserID", "DateDeleted", "ExportID"),
            "IncomeBenefits", List.of("IncomeBenefitsID", "EnrollmentID", "PersonalID", "InformationDate", "IncomeFromAnySource", "TotalMonthlyIncome", "Earned", "EarnedAmount", "Unemployment", "UnemploymentAmount", "SSI", "SSIAmount", "SSDI", "SSDIAmount", "VADisabilityService", "VADisabilityServiceAmount", "VADisabilityNonService", "VADisabilityNonServiceAmount", "PrivateDisability", "PrivateDisabilityAmount", "WorkersComp", "WorkersCompAmount", "TANF", "TANFAmount", "GA", "GAAmount", "SocSecRetirement", "SocSecRetirementAmount", "Pension", "PensionAmount", "ChildSupport", "ChildSupportAmount", "Alimony", "AlimonyAmount", "OtherIncomeSource", "OtherIncomeAmount", "OtherIncomeSourceIdentify", "BenefitsFromAnySource", "SNAP", "WIC", "TANFChildCare", "TANFTransportation", "OtherTANF", "OtherBenefitsSource", "OtherBenefitsSourceIdentify", "InsuranceFromAnySource", "Medicaid", "Medicare", "SCHIP", "VAMedicalServices", "EmployerProvided", "COBRA", "PrivatePay", "StateHealthIns", "IndianHealthServices", "OtherInsurance", "OtherInsuranceIdentify", "DataCollectionStage", "DateCreated", "DateUpdated", "UserID", "DateDeleted", "ExportID"),
            "HealthAndDV", List.of("HealthAndDVID", "EnrollmentID", "PersonalID", "InformationDate", "DomesticViolenceVictim", "WhenOccurred", "CurrentlyFleeing", "GeneralHealthStatus", "DentalHealthStatus", "MentalHealthStatus", "PregnancyStatus", "DueDate", "DataCollectionStage", "DateCreated", "DateUpdated", "UserID", "DateDeleted", "ExportID"),
            "Disabilities", List.of("DisabilitiesID", "EnrollmentID", "PersonalID", "InformationDate", "DisabilityType", "DisabilityResponse", "IndefiniteAndImpairs", "TCellCount", "TCellSource", "ViralLoadAvailable", "ViralLoad", "ViralLoadSource", "AntiRetroviral", "DataCollectionStage", "DateCreated", "DateUpdated", "UserID", "DateDeleted", "ExportID")
    );

    @Override
    public byte[] format(Map<String, List<Map<String, Object>>> sections) {
        return formatWithValidation(sections, null, null, null);
    }

    /**
     * Format CSV with comprehensive validation guardrails.
     *
     * @param sections CSV section data
     * @param exportPeriod Export period for date range validation
     * @param exportJobId Export job identifier for diagnostic logging
     * @return CSV byte array
     * @throws CsvValidationException if validation errors prevent export
     */
    public byte[] formatWithValidation(
            Map<String, List<Map<String, Object>>> sections,
            ExportPeriod exportPeriod,
            String exportJobId,
            CsvValidationLogger validationLogger) {

        // Initialize validation logger if not provided
        CsvValidationLogger logger = validationLogger != null
                ? validationLogger
                : new CsvValidationLogger(exportJobId != null ? exportJobId : "unknown");

        LocalDate exportStartDate = exportPeriod != null ? exportPeriod.startDate() : null;
        LocalDate exportEndDate = exportPeriod != null ? exportPeriod.endDate() : null;

        CSVExportStrategy.logger.info("Starting CSV export with validation for {} sections", sections.size());

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream()) {
            // Write UTF-8 BOM
            baos.write(UTF8_BOM);

            try (Writer writer = new OutputStreamWriter(baos, StandardCharsets.UTF_8)) {
                for (Map.Entry<String, List<Map<String, Object>>> section : sections.entrySet()) {
                    String sectionName = section.getKey();
                    List<Map<String, Object>> rows = section.getValue();

                    if (rows.isEmpty()) {
                        continue;
                    }

                    CSVExportStrategy.logger.debug("Processing section: {} with {} rows", sectionName, rows.size());

                    // Determine column order (use HUD spec or inferred)
                    List<String> columnOrder = getColumnOrder(sectionName, rows.get(0).keySet());

                    // Write header
                    writeRow(writer, columnOrder.stream().map(this::escapeField).collect(Collectors.toList()));

                    // Validate and write data rows
                    for (int rowIndex = 0; rowIndex < rows.size(); rowIndex++) {
                        Map<String, Object> row = rows.get(rowIndex);
                        String rowContext = sectionName + " row " + (rowIndex + 1);

                        // Invoke validation prior to row emission
                        List<ValidationDiagnostic> rowDiagnostics = validateRow(
                                sectionName,
                                row,
                                columnOrder,
                                exportStartDate,
                                exportEndDate,
                                rowContext
                        );

                        // Propagate diagnostic results to structured log channel
                        logger.logBatch(rowDiagnostics);

                        // Reject row if validation errors exist
                        boolean hasErrors = rowDiagnostics.stream()
                                .anyMatch(ValidationDiagnostic::isError);

                        if (hasErrors) {
                            CSVExportStrategy.logger.error(
                                    "Row rejected due to validation errors: {} (errors: {})",
                                    rowContext,
                                    rowDiagnostics.stream()
                                            .filter(ValidationDiagnostic::isError)
                                            .count()
                            );
                            continue; // Skip row - do not emit
                        }

                        // Write validated row
                        List<String> values = columnOrder.stream()
                                .map(col -> formatValue(row.get(col)))
                                .map(this::escapeField)
                                .collect(Collectors.toList());
                        writeRow(writer, values);
                    }

                    // Add blank line between sections (if multiple sections)
                    if (sections.size() > 1) {
                        writer.write(LINE_SEPARATOR);
                    }
                }
            }

            // Log validation summary
            logger.logSummary();

            // Fail export if critical errors exist
            if (logger.hasErrors()) {
                CSVExportStrategy.logger.error(
                        "CSV export failed validation with {} errors",
                        logger.getErrorCount()
                );
                throw new CsvValidationException(
                        "CSV export validation failed with " + logger.getErrorCount() + " errors",
                        logger.getSummary()
                );
            }

            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Failed to generate CSV export", e);
        }
    }

    /**
     * Validates a single CSV row against HUD business rules.
     *
     * @param sectionName CSV section name (e.g., "Enrollment")
     * @param row Row data
     * @param columnOrder Ordered column names
     * @param exportStartDate Export period start (nullable)
     * @param exportEndDate Export period end (nullable)
     * @param rowContext Anonymized row identifier
     * @return List of validation diagnostics
     */
    private List<ValidationDiagnostic> validateRow(
            String sectionName,
            Map<String, Object> row,
            List<String> columnOrder,
            LocalDate exportStartDate,
            LocalDate exportEndDate,
            String rowContext) {

        List<ValidationDiagnostic> diagnostics = new ArrayList<>();

        // Section-specific validation rules
        switch (sectionName) {
            case "Client" -> diagnostics.addAll(validateClientRow(row, rowContext));
            case "Enrollment" -> diagnostics.addAll(validateEnrollmentRow(row, exportStartDate, exportEndDate, rowContext));
            case "Exit" -> diagnostics.addAll(validateExitRow(row, exportStartDate, exportEndDate, rowContext));
            case "Services" -> diagnostics.addAll(validateServicesRow(row, exportStartDate, exportEndDate, rowContext));
            case "IncomeBenefits" -> diagnostics.addAll(validateIncomeBenefitsRow(row, exportStartDate, exportEndDate, rowContext));
            case "HealthAndDV" -> diagnostics.addAll(validateHealthAndDVRow(row, exportStartDate, exportEndDate, rowContext));
            case "Disabilities" -> diagnostics.addAll(validateDisabilitiesRow(row, exportStartDate, exportEndDate, rowContext));
            default -> CSVExportStrategy.logger.debug("No specific validation for section: {}", sectionName);
        }

        return diagnostics;
    }

    private List<ValidationDiagnostic> validateClientRow(Map<String, Object> row, String rowContext) {
        List<ValidationDiagnostic> diagnostics = new ArrayList<>();

        // Name validation
        diagnostics.add(CsvValidationUtilities.validateNullableField("FirstName", row.get("FirstName"), "R", rowContext));
        diagnostics.add(CsvValidationUtilities.validateNullableField("LastName", row.get("LastName"), "R", rowContext));
        diagnostics.add(CsvValidationUtilities.validatePicklistCode("NameDataQuality", row.get("NameDataQuality"),
                HudPicklistCodes.NAME_DATA_QUALITY, "1.4 Name Data Quality", rowContext));

        // SSN validation
        diagnostics.add(CsvValidationUtilities.validatePicklistCode("SSNDataQuality", row.get("SSNDataQuality"),
                HudPicklistCodes.SSN_DATA_QUALITY, "1.5 SSN Data Quality", rowContext));

        // DOB validation
        diagnostics.add(CsvValidationUtilities.validatePicklistCode("DOBDataQuality", row.get("DOBDataQuality"),
                HudPicklistCodes.DOB_DATA_QUALITY, "1.6 DOB Data Quality", rowContext));

        return diagnostics.stream().filter(d -> !d.isSuccess()).collect(Collectors.toList());
    }

    private List<ValidationDiagnostic> validateEnrollmentRow(
            Map<String, Object> row,
            LocalDate exportStartDate,
            LocalDate exportEndDate,
            String rowContext) {

        List<ValidationDiagnostic> diagnostics = new ArrayList<>();

        // EntryDate validation
        diagnostics.add(CsvValidationUtilities.validateDateInRange(
                "EntryDate", row.get("EntryDate"), exportStartDate, exportEndDate, rowContext));

        // RelationshipToHoH validation
        diagnostics.add(CsvValidationUtilities.validateNullableField("RelationshipToHoH", row.get("RelationshipToHoH"), "R", rowContext));
        diagnostics.add(CsvValidationUtilities.validatePicklistCode("RelationshipToHoH", row.get("RelationshipToHoH"),
                HudPicklistCodes.RELATIONSHIP_TO_HOH, "1.27 Relationship to HoH", rowContext));

        // DisablingCondition validation
        diagnostics.add(CsvValidationUtilities.validatePicklistCode("DisablingCondition", row.get("DisablingCondition"),
                HudPicklistCodes.DISABLING_CONDITION, "1.7 Disabling Condition", rowContext));

        // LivingSituation validation
        diagnostics.add(CsvValidationUtilities.validateNullableField("LivingSituation", row.get("LivingSituation"), "R", rowContext));
        diagnostics.add(CsvValidationUtilities.validatePicklistCode("LivingSituation", row.get("LivingSituation"),
                HudPicklistCodes.LIVING_SITUATION, "3.917 Living Situation", rowContext));

        return diagnostics.stream().filter(d -> !d.isSuccess()).collect(Collectors.toList());
    }

    private List<ValidationDiagnostic> validateExitRow(
            Map<String, Object> row,
            LocalDate exportStartDate,
            LocalDate exportEndDate,
            String rowContext) {

        List<ValidationDiagnostic> diagnostics = new ArrayList<>();

        // ExitDate validation
        diagnostics.add(CsvValidationUtilities.validateDateInRange(
                "ExitDate", row.get("ExitDate"), exportStartDate, exportEndDate, rowContext));

        // Destination validation
        diagnostics.add(CsvValidationUtilities.validateNullableField("Destination", row.get("Destination"), "R", rowContext));
        diagnostics.add(CsvValidationUtilities.validatePicklistCode("Destination", row.get("Destination"),
                HudPicklistCodes.DESTINATION, "3.12 Destination", rowContext));

        return diagnostics.stream().filter(d -> !d.isSuccess()).collect(Collectors.toList());
    }

    private List<ValidationDiagnostic> validateServicesRow(
            Map<String, Object> row,
            LocalDate exportStartDate,
            LocalDate exportEndDate,
            String rowContext) {

        List<ValidationDiagnostic> diagnostics = new ArrayList<>();

        // DateProvided validation
        diagnostics.add(CsvValidationUtilities.validateDateInRange(
                "DateProvided", row.get("DateProvided"), exportStartDate, exportEndDate, rowContext));

        // RecordType validation
        diagnostics.add(CsvValidationUtilities.validateNullableField("RecordType", row.get("RecordType"), "R", rowContext));
        diagnostics.add(CsvValidationUtilities.validatePicklistCode("RecordType", row.get("RecordType"),
                HudPicklistCodes.RECORD_TYPE, "Record Type", rowContext));

        return diagnostics.stream().filter(d -> !d.isSuccess()).collect(Collectors.toList());
    }

    private List<ValidationDiagnostic> validateIncomeBenefitsRow(
            Map<String, Object> row,
            LocalDate exportStartDate,
            LocalDate exportEndDate,
            String rowContext) {

        List<ValidationDiagnostic> diagnostics = new ArrayList<>();

        // InformationDate validation
        diagnostics.add(CsvValidationUtilities.validateDateInRange(
                "InformationDate", row.get("InformationDate"), exportStartDate, exportEndDate, rowContext));

        // DataCollectionStage validation
        diagnostics.add(CsvValidationUtilities.validateNullableField("DataCollectionStage", row.get("DataCollectionStage"), "R", rowContext));
        diagnostics.add(CsvValidationUtilities.validatePicklistCode("DataCollectionStage", row.get("DataCollectionStage"),
                HudPicklistCodes.DATA_COLLECTION_STAGE, "4.05 Data Collection Stage", rowContext));

        return diagnostics.stream().filter(d -> !d.isSuccess()).collect(Collectors.toList());
    }

    private List<ValidationDiagnostic> validateHealthAndDVRow(
            Map<String, Object> row,
            LocalDate exportStartDate,
            LocalDate exportEndDate,
            String rowContext) {

        List<ValidationDiagnostic> diagnostics = new ArrayList<>();

        // InformationDate validation
        diagnostics.add(CsvValidationUtilities.validateDateInRange(
                "InformationDate", row.get("InformationDate"), exportStartDate, exportEndDate, rowContext));

        // DomesticViolenceVictim validation
        diagnostics.add(CsvValidationUtilities.validatePicklistCode("DomesticViolenceVictim", row.get("DomesticViolenceVictim"),
                HudPicklistCodes.DOMESTIC_VIOLENCE, "4.10 Domestic Violence", rowContext));

        // WhenOccurred validation (conditional)
        diagnostics.add(CsvValidationUtilities.validatePicklistCode("WhenOccurred", row.get("WhenOccurred"),
                HudPicklistCodes.WHEN_DV_OCCURRED, "4.10.2 When DV Occurred", rowContext));

        return diagnostics.stream().filter(d -> !d.isSuccess()).collect(Collectors.toList());
    }

    private List<ValidationDiagnostic> validateDisabilitiesRow(
            Map<String, Object> row,
            LocalDate exportStartDate,
            LocalDate exportEndDate,
            String rowContext) {

        List<ValidationDiagnostic> diagnostics = new ArrayList<>();

        // InformationDate validation
        diagnostics.add(CsvValidationUtilities.validateDateInRange(
                "InformationDate", row.get("InformationDate"), exportStartDate, exportEndDate, rowContext));

        // DisabilityType validation
        diagnostics.add(CsvValidationUtilities.validateNullableField("DisabilityType", row.get("DisabilityType"), "R", rowContext));
        diagnostics.add(CsvValidationUtilities.validatePicklistCode("DisabilityType", row.get("DisabilityType"),
                HudPicklistCodes.DISABILITY_TYPE, "4.12 Disability Type", rowContext));

        // DisabilityResponse validation
        diagnostics.add(CsvValidationUtilities.validateNullableField("DisabilityResponse", row.get("DisabilityResponse"), "R", rowContext));
        diagnostics.add(CsvValidationUtilities.validatePicklistCode("DisabilityResponse", row.get("DisabilityResponse"),
                HudPicklistCodes.DISABILITY_RESPONSE, "4.12 Disability Response", rowContext));

        return diagnostics.stream().filter(d -> !d.isSuccess()).collect(Collectors.toList());
    }

    private List<String> getColumnOrder(String sectionName, Set<String> availableColumns) {
        List<String> hudOrder = HUD_COLUMN_ORDER.get(sectionName);
        if (hudOrder != null) {
            // Use HUD-mandated order, filtering to available columns
            List<String> result = new ArrayList<>(hudOrder);
            result.retainAll(availableColumns);

            // Add any extra columns not in HUD spec (shouldn't happen in prod)
            availableColumns.stream()
                    .filter(col -> !result.contains(col))
                    .sorted()
                    .forEach(result::add);

            return result;
        }

        // Fallback to alphabetical for custom sections
        return availableColumns.stream().sorted().collect(Collectors.toList());
    }

    private void writeRow(Writer writer, List<String> fields) throws IOException {
        writer.write(String.join(FIELD_SEPARATOR, fields));
        writer.write(LINE_SEPARATOR);
    }

    private String formatValue(Object value) {
        if (value == null) {
            return "";
        }
        return value.toString();
    }

    /**
     * Escape field per RFC 4180:
     * - Quote if contains comma, quote, or line break
     * - Double internal quotes
     */
    private String escapeField(String field) {
        if (field == null) {
            return "";
        }

        boolean needsQuoting = field.contains(FIELD_SEPARATOR)
                || field.contains("\"")
                || field.contains("\r")
                || field.contains("\n");

        if (needsQuoting) {
            return "\"" + field.replace("\"", "\"\"") + "\"";
        }

        return field;
    }
}
