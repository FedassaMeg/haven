package org.haven.reporting.application.services;

import org.haven.reporting.domain.hmis.*;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * HMIS CSV Export Service
 * Generates standardized HMIS CSV exports for HUD reporting requirements.
 * Supports Client.csv, Enrollment.csv, Exit.csv, and other required files.
 * Aligned with HMIS 2024 Data Standards CSV schema.
 */
@Service
public class HmisCsvExportService {

    private static final String CSV_HEADER_CLIENT = 
        "PersonalID,FirstName,MiddleName,LastName,NameSuffix,NameDataQuality,SSN,SSNDataQuality," +
        "DOB,DOBDataQuality,Race,Gender,OtherGender,VeteranStatus,DateCreated,DateUpdated,UserID,DateDeleted,ExportID";

    private static final String CSV_HEADER_ENROLLMENT = 
        "EnrollmentID,PersonalID,ProjectID,EntryDate,HouseholdID,RelationshipToHoH," +
        "PriorLivingSituation,LengthOfStayPriorToDiEntry,EntryFromStreetESSH," +
        "MonthsHomelessPastThreeYears,TimesHomelessPastThreeYears,DisablingCondition," +
        "DateCreated,DateUpdated,UserID,DateDeleted,ExportID,PredecessorEnrollmentID,ResidentialMoveInDate";

    private static final String CSV_HEADER_EXIT = 
        "ExitID,EnrollmentID,PersonalID,ExitDate,Destination,OtherDestination," +
        "HousingAssessment,Subsidy,DateCreated,DateUpdated,UserID,DateDeleted,ExportID";

    private static final String CSV_HEADER_INCOME_BENEFITS =
        "IncomeBenefitsID,EnrollmentID,PersonalID,InformationDate,TotalMonthlyIncome," +
        "EarnedIncome,EarnedIncomeAmount,UnemploymentIncome,UnemploymentIncomeAmount," +
        "SSIIncome,SSIIncomeAmount,SSDIIncome,SSDIIncomeAmount,VADisabilityServiceIncome,VADisabilityServiceIncomeAmount," +
        "VADisabilityNonServiceIncome,VADisabilityNonServiceIncomeAmount,PrivateDisabilityIncome,PrivateDisabilityIncomeAmount," +
        "WorkersCompIncome,WorkersCompIncomeAmount,TANFIncome,TANFIncomeAmount,GAIncome,GAIncomeAmount," +
        "SocialSecurityRetirementIncome,SocialSecurityRetirementIncomeAmount,PensionIncome,PensionIncomeAmount," +
        "ChildSupportIncome,ChildSupportIncomeAmount,AlimonyIncome,AlimonyIncomeAmount," +
        "OtherIncomeSource,OtherIncomeAmount,SNAP,WIC,TANFChildCare,TANFTransportation,OtherTANF,OtherBenefitsSource," +
        "DateCreated,DateUpdated,UserID,DateDeleted,ExportID";

    private static final String CSV_HEADER_HEALTH_AND_DV =
        "HealthAndDVID,EnrollmentID,PersonalID,InformationDate,Medicaid,Medicare,SCHIP,VAMedicalServices," +
        "EmployerProvided,COBRA,PrivatePayment,StateHealthInsurance,IndianHealthService,OtherInsurance,NoInsurance," +
        "PhysicalDisability,DevelopmentalDisability,ChronicHealthCondition,HIVAIDS,MentalHealthDisorder,SubstanceUseDisorder," +
        "DomesticViolence,DateCreated,DateUpdated,UserID,DateDeleted,ExportID";

    /**
     * Generate complete HMIS CSV export as ZIP file
     */
    public byte[] generateHmisCsvExport(
            List<HmisClientProjection> clients,
            List<HmisEnrollmentProjection> enrollments,
            List<HmisExitProjection> exits,
            List<HmisIncomeBenefitsProjection> incomeBenefits,
            List<HmisHealthAndDvProjection> healthAndDv,
            String exportId,
            LocalDate reportingPeriodStart,
            LocalDate reportingPeriodEnd) throws IOException {
        
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zipOut = new ZipOutputStream(baos)) {
            
            // Export.csv - metadata about the export
            addExportMetadata(zipOut, exportId, reportingPeriodStart, reportingPeriodEnd);
            
            // Client.csv
            addClientsToZip(zipOut, clients);
            
            // Enrollment.csv
            addEnrollmentsToZip(zipOut, enrollments);
            
            // Exit.csv
            addExitsToZip(zipOut, exits);
            
            // IncomeBenefits.csv
            addIncomeBenefitsToZip(zipOut, incomeBenefits);
            
            // HealthAndDV.csv
            addHealthAndDvToZip(zipOut, healthAndDv);
            
            zipOut.finish();
            return baos.toByteArray();
        }
    }

    /**
     * Generate Client.csv export
     */
    public String generateClientCsv(List<HmisClientProjection> clients) {
        StringBuilder csv = new StringBuilder();
        csv.append(CSV_HEADER_CLIENT).append("\n");
        
        for (HmisClientProjection client : clients) {
            csv.append(client.toCsvRow()).append("\n");
        }
        
        return csv.toString();
    }

    /**
     * Generate Enrollment.csv export
     */
    public String generateEnrollmentCsv(List<HmisEnrollmentProjection> enrollments) {
        StringBuilder csv = new StringBuilder();
        csv.append(CSV_HEADER_ENROLLMENT).append("\n");
        
        for (HmisEnrollmentProjection enrollment : enrollments) {
            csv.append(enrollment.toCsvRow()).append("\n");
        }
        
        return csv.toString();
    }

    /**
     * Generate Exit.csv export
     */
    public String generateExitCsv(List<HmisExitProjection> exits) {
        StringBuilder csv = new StringBuilder();
        csv.append(CSV_HEADER_EXIT).append("\n");
        
        for (HmisExitProjection exit : exits) {
            csv.append(exit.toCsvRow()).append("\n");
        }
        
        return csv.toString();
    }
    
    /**
     * Generate IncomeBenefits.csv export
     */
    public String generateIncomeBenefitsCsv(List<HmisIncomeBenefitsProjection> incomeBenefits) {
        StringBuilder csv = new StringBuilder();
        csv.append(CSV_HEADER_INCOME_BENEFITS).append("\n");
        
        for (HmisIncomeBenefitsProjection incomeBenefit : incomeBenefits) {
            csv.append(incomeBenefit.toCsvRow()).append("\n");
        }
        
        return csv.toString();
    }
    
    /**
     * Generate HealthAndDV.csv export
     */
    public String generateHealthAndDvCsv(List<HmisHealthAndDvProjection> healthAndDv) {
        StringBuilder csv = new StringBuilder();
        csv.append(CSV_HEADER_HEALTH_AND_DV).append("\n");
        
        for (HmisHealthAndDvProjection healthDv : healthAndDv) {
            csv.append(healthDv.toCsvRow()).append("\n");
        }
        
        return csv.toString();
    }

    private void addExportMetadata(ZipOutputStream zipOut, String exportId, 
                                  LocalDate startDate, LocalDate endDate) throws IOException {
        ZipEntry exportEntry = new ZipEntry("Export.csv");
        zipOut.putNextEntry(exportEntry);
        
        try (PrintWriter writer = new PrintWriter(zipOut)) {
            writer.println("ExportID,SourceContactFirst,SourceContactLast,SourceContactPhone," +
                          "SourceContactExtension,SourceContactEmail,ExportDate,ExportStartDate," +
                          "ExportEndDate,SoftwareName,SoftwareVersion,ExportPeriodType,ExportDirective");
            
            writer.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                exportId,
                "System", // SourceContactFirst
                "Administrator", // SourceContactLast
                "", // SourceContactPhone
                "", // SourceContactExtension
                "", // SourceContactEmail
                LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE), // ExportDate
                startDate.format(DateTimeFormatter.ISO_LOCAL_DATE), // ExportStartDate
                endDate.format(DateTimeFormatter.ISO_LOCAL_DATE), // ExportEndDate
                "Haven HMIS", // SoftwareName
                "1.0", // SoftwareVersion
                "3", // ExportPeriodType (Operating Year)
                "1" // ExportDirective (Standard Export)
            );
        }
        
        zipOut.closeEntry();
    }

    private void addClientsToZip(ZipOutputStream zipOut, List<HmisClientProjection> clients) throws IOException {
        ZipEntry clientEntry = new ZipEntry("Client.csv");
        zipOut.putNextEntry(clientEntry);
        
        try (PrintWriter writer = new PrintWriter(zipOut)) {
            writer.println(CSV_HEADER_CLIENT);
            for (HmisClientProjection client : clients) {
                writer.println(client.toCsvRow());
            }
        }
        
        zipOut.closeEntry();
    }

    private void addEnrollmentsToZip(ZipOutputStream zipOut, List<HmisEnrollmentProjection> enrollments) throws IOException {
        ZipEntry enrollmentEntry = new ZipEntry("Enrollment.csv");
        zipOut.putNextEntry(enrollmentEntry);
        
        try (PrintWriter writer = new PrintWriter(zipOut)) {
            writer.println(CSV_HEADER_ENROLLMENT);
            for (HmisEnrollmentProjection enrollment : enrollments) {
                writer.println(enrollment.toCsvRow());
            }
        }
        
        zipOut.closeEntry();
    }

    private void addExitsToZip(ZipOutputStream zipOut, List<HmisExitProjection> exits) throws IOException {
        ZipEntry exitEntry = new ZipEntry("Exit.csv");
        zipOut.putNextEntry(exitEntry);
        
        try (PrintWriter writer = new PrintWriter(zipOut)) {
            writer.println(CSV_HEADER_EXIT);
            for (HmisExitProjection exit : exits) {
                writer.println(exit.toCsvRow());
            }
        }
        
        zipOut.closeEntry();
    }

    private void addIncomeBenefitsToZip(ZipOutputStream zipOut, List<HmisIncomeBenefitsProjection> incomeBenefits) throws IOException {
        ZipEntry incomeBenefitsEntry = new ZipEntry("IncomeBenefits.csv");
        zipOut.putNextEntry(incomeBenefitsEntry);
        
        try (PrintWriter writer = new PrintWriter(zipOut)) {
            writer.println(CSV_HEADER_INCOME_BENEFITS);
            for (HmisIncomeBenefitsProjection incomeBenefit : incomeBenefits) {
                writer.println(incomeBenefit.toCsvRow());
            }
        }
        
        zipOut.closeEntry();
    }

    private void addHealthAndDvToZip(ZipOutputStream zipOut, List<HmisHealthAndDvProjection> healthAndDv) throws IOException {
        ZipEntry healthAndDvEntry = new ZipEntry("HealthAndDV.csv");
        zipOut.putNextEntry(healthAndDvEntry);
        
        try (PrintWriter writer = new PrintWriter(zipOut)) {
            writer.println(CSV_HEADER_HEALTH_AND_DV);
            for (HmisHealthAndDvProjection healthDv : healthAndDv) {
                writer.println(healthDv.toCsvRow());
            }
        }
        
        zipOut.closeEntry();
    }
}