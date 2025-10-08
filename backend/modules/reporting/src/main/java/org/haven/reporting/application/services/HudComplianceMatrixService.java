package org.haven.reporting.application.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import org.haven.shared.vo.hmis.*;
import org.springframework.stereotype.Service;

import java.io.StringWriter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * HUD Compliance Matrix Service
 * Builds a canonical inventory of HUD/HMIS elements and tracks their implementation
 * across domain aggregates, API routes, and UI widgets.
 */
@Service
public class HudComplianceMatrixService {

    private final ObjectMapper yamlMapper = new ObjectMapper(new YAMLFactory());
    private final ObjectMapper jsonMapper = new ObjectMapper();

    /**
     * Generate the complete HUD compliance matrix
     */
    public HudComplianceMatrix generateComplianceMatrix() {
        List<HudDataElement> hudElements = buildCanonicalHudElementInventory();
        
        return new HudComplianceMatrix(
            "2024.1.0",
            new Date(),
            hudElements,
            calculateOverallComplianceScore(hudElements),
            buildComplianceSummary(hudElements)
        );
    }

    /**
     * Build canonical inventory of all HUD/HMIS data elements
     */
    private List<HudDataElement> buildCanonicalHudElementInventory() {
        List<HudDataElement> elements = new ArrayList<>();
        
        // Universal Data Elements (3.01 - 3.07)
        elements.addAll(buildUniversalDataElements());
        
        // Program Entry Data Elements (3.10 - 3.15)
        elements.addAll(buildProgramEntryDataElements());
        
        // Project Exit Data Elements (3.16 - 3.20)
        elements.addAll(buildProjectExitDataElements());
        
        // Income and Benefits Data Elements (4.01 - 4.10)
        elements.addAll(buildIncomeAndBenefitsDataElements());
        
        // Health and Domestic Violence Data Elements (4.11 - 4.13)
        elements.addAll(buildHealthAndDomesticViolenceDataElements());
        
        return elements;
    }

    private List<HudDataElement> buildUniversalDataElements() {
        return List.of(
            new HudDataElement(
                "3.01",
                "Personal ID",
                "Personal ID",
                true,
                HudElementCategory.UNIVERSAL,
                "Client",
                "HmisPersonalId",
                new DomainImplementation("Client", "getHmisPersonalId()", true),
                new ApiImplementation("/clients/{id}", "GET", "personalId", true),
                new UiImplementation("ClientDetails", "personalId", true),
                "HMIS Personal ID generated and managed"
            ),
            new HudDataElement(
                "3.02",
                "First Name",
                "First Name",
                true,
                HudElementCategory.UNIVERSAL,
                "Client",
                "HumanName",
                new DomainImplementation("Client", "getPrimaryName().given().get(0)", true),
                new ApiImplementation("/clients/{id}", "GET", "name.given[0]", true),
                new UiImplementation("ClientDetails", "firstName", true),
                "First name collected and stored"
            ),
            new HudDataElement(
                "3.03",
                "Last Name",
                "Last Name",
                true,
                HudElementCategory.UNIVERSAL,
                "Client",
                "HumanName",
                new DomainImplementation("Client", "getPrimaryName().family()", true),
                new ApiImplementation("/clients/{id}", "GET", "name.family", true),
                new UiImplementation("ClientDetails", "lastName", true),
                "Last name collected and stored"
            ),
            new HudDataElement(
                "3.04",
                "Race",
                "Race",
                true,
                HudElementCategory.UNIVERSAL,
                "Client",
                "Set<HmisRace>",
                new DomainImplementation("Client", "getHmisRace()", true),
                new ApiImplementation("/clients/{id}", "GET", "race", true),
                new UiImplementation("DemographicsForm", "race", true),
                "HMIS 2024-compliant race categories"
            ),
            new HudDataElement(
                "3.05",
                "Ethnicity",
                "Ethnicity",
                false,
                HudElementCategory.UNIVERSAL,
                "Client",
                "HmisEthnicity",
                new DomainImplementation("Client", "getHmisEthnicity()", false),
                new ApiImplementation("/clients/{id}", "GET", "ethnicity", false),
                new UiImplementation("DemographicsForm", "ethnicity", false),
                "Not yet implemented - requires domain extension"
            ),
            new HudDataElement(
                "3.06",
                "Gender",
                "Gender",
                true,
                HudElementCategory.UNIVERSAL,
                "Client",
                "Set<HmisGender>",
                new DomainImplementation("Client", "getHmisGender()", true),
                new ApiImplementation("/clients/{id}", "GET", "gender", true),
                new UiImplementation("DemographicsForm", "gender", true),
                "HMIS 2024-compliant gender categories"
            ),
            new HudDataElement(
                "3.07",
                "Veteran Status",
                "Veteran Status",
                true,
                HudElementCategory.UNIVERSAL,
                "Client",
                "VeteranStatus",
                new DomainImplementation("Client", "getVeteranStatus()", true),
                new ApiImplementation("/clients/{id}", "GET", "veteranStatus", true),
                new UiImplementation("DemographicsForm", "veteranStatus", true),
                "Veteran status collected for adults 18+"
            )
        );
    }

    private List<HudDataElement> buildProgramEntryDataElements() {
        return List.of(
            new HudDataElement(
                "3.10",
                "Relationship to Head of Household",
                "Relationship to Head of Household",
                true,
                HudElementCategory.PROGRAM_ENTRY,
                "ProgramEnrollment",
                "RelationshipToHeadOfHousehold",
                new DomainImplementation("ProgramEnrollment", "getHmisRelationshipToHoH()", true),
                new ApiImplementation("/api/v1/enrollments/{id}/chain", "GET", "householdRole", true),
                new UiImplementation("EnrollmentForm", "relationshipToHoH", true),
                "Household composition tracked"
            ),
            new HudDataElement(
                "3.11",
                "Prior Living Situation",
                "Prior Living Situation",
                true,
                HudElementCategory.PROGRAM_ENTRY,
                "ProgramEnrollment",
                "PriorLivingSituation",
                new DomainImplementation("ProgramEnrollment", "getHmisPriorLivingSituation()", true),
                new ApiImplementation("/api/v1/enrollments/{id}/chain", "GET", "priorLivingSituation", true),
                new UiImplementation("EnrollmentForm", "priorLivingSituation", true),
                "Prior living situation documented"
            ),
            new HudDataElement(
                "3.12",
                "Length of Stay",
                "Length of Stay",
                true,
                HudElementCategory.PROGRAM_ENTRY,
                "ProgramEnrollment",
                "LengthOfStay",
                new DomainImplementation("ProgramEnrollment", "getHmisLengthOfStay()", true),
                new ApiImplementation("/api/v1/enrollments/{id}/chain", "GET", "lengthOfStay", true),
                new UiImplementation("EnrollmentForm", "lengthOfStay", true),
                "Length of stay in prior situation"
            ),
            new HudDataElement(
                "3.13",
                "Disabling Condition",
                "Disabling Condition",
                true,
                HudElementCategory.PROGRAM_ENTRY,
                "ProgramEnrollment",
                "DisablingCondition",
                new DomainImplementation("ProgramEnrollment", "getHmisDisablingCondition()", true),
                new ApiImplementation("/api/v1/enrollments/{id}/chain", "GET", "disablingCondition", true),
                new UiImplementation("EnrollmentForm", "disablingCondition", true),
                "Disabling condition assessment"
            ),
            new HudDataElement(
                "3.15",
                "Residential Move-in Date",
                "Residential Move-in Date",
                true,
                HudElementCategory.PROGRAM_ENTRY,
                "ProgramEnrollment",
                "LocalDate",
                new DomainImplementation("ProgramEnrollment", "getResidentialMoveInDate()", true),
                new ApiImplementation("/api/v1/enrollments/{id}/move-in-date", "PATCH", "moveInDate", true),
                new UiImplementation("EnrollmentForm", "moveInDate", true),
                "Move-in date for RRH/PSH projects"
            )
        );
    }

    private List<HudDataElement> buildProjectExitDataElements() {
        return List.of(
            new HudDataElement(
                "3.16",
                "Project Exit Date",
                "Project Exit Date",
                true,
                HudElementCategory.PROJECT_EXIT,
                "ProjectExit",
                "LocalDate",
                new DomainImplementation("ProjectExit", "getExitDate()", true),
                new ApiImplementation("/api/v1/enrollments/{id}/exit", "POST", "exitDate", true),
                new UiImplementation("ExitForm", "exitDate", true),
                "Project exit date recorded"
            ),
            new HudDataElement(
                "3.17",
                "Destination",
                "Destination",
                true,
                HudElementCategory.PROJECT_EXIT,
                "ProjectExit",
                "ProjectExitDestination",
                new DomainImplementation("ProjectExit", "getHmisExitDestination()", true),
                new ApiImplementation("/api/v1/enrollments/{id}/exit", "POST", "destination", true),
                new UiImplementation("ExitForm", "destination", true),
                "Exit destination documented"
            )
        );
    }

    private List<HudDataElement> buildIncomeAndBenefitsDataElements() {
        return List.of(
            new HudDataElement(
                "4.01",
                "Income from Any Source",
                "Income from Any Source",
                false,
                HudElementCategory.INCOME_BENEFITS,
                "IncomeAssessment",
                "IncomeFromAnySource",
                new DomainImplementation("IncomeAssessment", "getIncomeFromAnySource()", false),
                new ApiImplementation("/api/v1/income/{id}", "GET", "incomeFromAnySource", false),
                new UiImplementation("IncomeForm", "incomeFromAnySource", false),
                "Not yet implemented - requires income module"
            ),
            new HudDataElement(
                "4.02",
                "Income Sources",
                "Income Sources",
                false,
                HudElementCategory.INCOME_BENEFITS,
                "IncomeAssessment",
                "Set<IncomeSource>",
                new DomainImplementation("IncomeAssessment", "getIncomeSources()", false),
                new ApiImplementation("/api/v1/income/{id}", "GET", "incomeSources", false),
                new UiImplementation("IncomeForm", "incomeSources", false),
                "Not yet implemented - requires income module"
            ),
            new HudDataElement(
                "4.03",
                "Non-Cash Benefits",
                "Non-Cash Benefits",
                false,
                HudElementCategory.INCOME_BENEFITS,
                "BenefitsAssessment",
                "Set<NonCashBenefit>",
                new DomainImplementation("BenefitsAssessment", "getNonCashBenefits()", false),
                new ApiImplementation("/api/v1/benefits/{id}", "GET", "nonCashBenefits", false),
                new UiImplementation("BenefitsForm", "nonCashBenefits", false),
                "Not yet implemented - requires benefits module"
            )
        );
    }

    private List<HudDataElement> buildHealthAndDomesticViolenceDataElements() {
        return List.of(
            new HudDataElement(
                "4.11",
                "Health Insurance",
                "Health Insurance",
                false,
                HudElementCategory.HEALTH_DV,
                "HealthAssessment",
                "Set<HealthInsurance>",
                new DomainImplementation("HealthAssessment", "getHealthInsurance()", false),
                new ApiImplementation("/api/v1/health/{id}", "GET", "healthInsurance", false),
                new UiImplementation("HealthForm", "healthInsurance", false),
                "Not yet implemented - requires health module"
            ),
            new HudDataElement(
                "4.12",
                "Domestic Violence",
                "Domestic Violence",
                true,
                HudElementCategory.HEALTH_DV,
                "DomesticViolenceAssessment",
                "DomesticViolence",
                new DomainImplementation("DomesticViolenceAssessment", "getDomesticViolence()", true),
                new ApiImplementation("/api/v1/enrollments/{id}/dv", "GET", "domesticViolence", true),
                new UiImplementation("DvForm", "domesticViolence", true),
                "Domestic violence assessment available"
            ),
            new HudDataElement(
                "4.13",
                "Disabilities",
                "Disabilities",
                true,
                HudElementCategory.HEALTH_DV,
                "DisabilityAssessment",
                "Set<DisabilityType>",
                new DomainImplementation("DisabilityAssessment", "getDisabilityTypes()", true),
                new ApiImplementation("/api/v1/enrollments/{id}/disabilities", "GET", "disabilities", true),
                new UiImplementation("DisabilityForm", "disabilities", true),
                "Disability assessment implemented"
            )
        );
    }

    private double calculateOverallComplianceScore(List<HudDataElement> elements) {
        long implementedCount = elements.stream()
            .mapToLong(element -> {
                boolean domainImplemented = element.domainImplementation().implemented();
                boolean apiImplemented = element.apiImplementation().implemented();
                boolean uiImplemented = element.uiImplementation().implemented();
                
                // All three layers must be implemented for full compliance
                if (domainImplemented && apiImplemented && uiImplemented) {
                    return 3; // Fully implemented
                } else if (domainImplemented && apiImplemented) {
                    return 2; // Backend implemented
                } else if (domainImplemented) {
                    return 1; // Domain only
                } else {
                    return 0; // Not implemented
                }
            })
            .sum();
        
        long totalPossible = elements.size() * 3L; // 3 layers per element
        return totalPossible > 0 ? (double) implementedCount / totalPossible * 100.0 : 0.0;
    }

    private ComplianceSummary buildComplianceSummary(List<HudDataElement> elements) {
        Map<HudElementCategory, List<HudDataElement>> byCategory = elements.stream()
            .collect(Collectors.groupingBy(HudDataElement::category));
        
        Map<HudElementCategory, ComplianceByCategory> categoryCompliance = byCategory.entrySet().stream()
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                entry -> buildCategoryCompliance(entry.getValue())
            ));
        
        long totalElements = elements.size();
        long fullyImplemented = elements.stream()
            .mapToLong(element -> 
                element.domainImplementation().implemented() && 
                element.apiImplementation().implemented() && 
                element.uiImplementation().implemented() ? 1 : 0)
            .sum();
        
        long partiallyImplemented = elements.stream()
            .mapToLong(element -> {
                boolean domain = element.domainImplementation().implemented();
                boolean api = element.apiImplementation().implemented();
                boolean ui = element.uiImplementation().implemented();
                return (domain || api || ui) && !(domain && api && ui) ? 1 : 0;
            })
            .sum();
        
        return new ComplianceSummary(
            totalElements,
            fullyImplemented,
            partiallyImplemented,
            totalElements - fullyImplemented - partiallyImplemented,
            categoryCompliance
        );
    }

    private ComplianceByCategory buildCategoryCompliance(List<HudDataElement> elements) {
        long total = elements.size();
        long implemented = elements.stream()
            .mapToLong(element -> 
                element.domainImplementation().implemented() && 
                element.apiImplementation().implemented() && 
                element.uiImplementation().implemented() ? 1 : 0)
            .sum();
        
        double percentage = total > 0 ? (double) implemented / total * 100.0 : 0.0;
        
        return new ComplianceByCategory(total, implemented, percentage);
    }

    /**
     * Export matrix as YAML
     */
    public String exportAsYaml(HudComplianceMatrix matrix) {
        try {
            StringWriter writer = new StringWriter();
            yamlMapper.writeValue(writer, matrix);
            return writer.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to export matrix as YAML", e);
        }
    }

    /**
     * Export matrix as JSON
     */
    public String exportAsJson(HudComplianceMatrix matrix) {
        try {
            return jsonMapper.writerWithDefaultPrettyPrinter().writeValueAsString(matrix);
        } catch (Exception e) {
            throw new RuntimeException("Failed to export matrix as JSON", e);
        }
    }

    /**
     * Validate that all mandatory HUD elements have implementation entries
     */
    public MatrixValidationResult validateMatrix(HudComplianceMatrix matrix) {
        List<String> violations = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        
        for (HudDataElement element : matrix.hudElements()) {
            if (element.mandatory()) {
                if (!element.domainImplementation().implemented()) {
                    violations.add("Mandatory element " + element.hudId() + " missing domain implementation");
                }
                if (!element.apiImplementation().implemented()) {
                    violations.add("Mandatory element " + element.hudId() + " missing API implementation");
                }
                if (!element.uiImplementation().implemented()) {
                    warnings.add("Mandatory element " + element.hudId() + " missing UI implementation");
                }
            }
        }
        
        boolean passed = violations.isEmpty();
        return new MatrixValidationResult(passed, violations, warnings);
    }

    // Data Classes
    public record HudComplianceMatrix(
        String version,
        Date generatedAt,
        List<HudDataElement> hudElements,
        double overallComplianceScore,
        ComplianceSummary summary
    ) {}

    public record HudDataElement(
        String hudId,
        String name,
        String description,
        boolean mandatory,
        HudElementCategory category,
        String owningAggregate,
        String dataType,
        DomainImplementation domainImplementation,
        ApiImplementation apiImplementation,
        UiImplementation uiImplementation,
        String notes
    ) {}

    public record DomainImplementation(
        String aggregateClass,
        String method,
        boolean implemented
    ) {}

    public record ApiImplementation(
        String route,
        String httpMethod,
        String fieldName,
        boolean implemented
    ) {}

    public record UiImplementation(
        String componentName,
        String fieldName,
        boolean implemented
    ) {}

    public record ComplianceSummary(
        long totalElements,
        long fullyImplemented,
        long partiallyImplemented,
        long notImplemented,
        Map<HudElementCategory, ComplianceByCategory> byCategory
    ) {}

    public record ComplianceByCategory(
        long totalElements,
        long implementedElements,
        double compliancePercentage
    ) {}

    public record MatrixValidationResult(
        boolean passed,
        List<String> violations,
        List<String> warnings
    ) {}

    public enum HudElementCategory {
        UNIVERSAL("Universal Data Elements"),
        PROGRAM_ENTRY("Program Entry Data Elements"),
        PROJECT_EXIT("Project Exit Data Elements"),
        INCOME_BENEFITS("Income and Benefits Data Elements"),
        HEALTH_DV("Health and Domestic Violence Data Elements");

        private final String displayName;

        HudElementCategory(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }
}