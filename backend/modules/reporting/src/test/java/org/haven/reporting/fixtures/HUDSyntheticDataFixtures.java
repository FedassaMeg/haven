package org.haven.reporting.fixtures;

import java.time.LocalDate;
import java.util.*;

/**
 * Synthetic test data matching HUD FY2024 dataset structure.
 * Includes edge cases for VAWA, consent, overlapping enrollments.
 */
public class HUDSyntheticDataFixtures {

    /**
     * Generate complete synthetic dataset with typical distribution
     */
    public static Map<String, List<Map<String, Object>>> generateCompleteDataset() {
        Map<String, List<Map<String, Object>>> dataset = new HashMap<>();

        dataset.put("Client", generateClients());
        dataset.put("Project", generateProjects());
        dataset.put("Enrollment", generateEnrollments());
        dataset.put("Exit", generateExits());
        dataset.put("Services", generateServices());

        return dataset;
    }

    /**
     * Generate clients with edge cases
     */
    public static List<Map<String, Object>> generateClients() {
        List<Map<String, Object>> clients = new ArrayList<>();

        // Standard client
        clients.add(createClient(
                "C001", "Jane", "Doe", "123456789",
                LocalDate.of(1985, 3, 15), 1, 1, false, true, false, false, false,
                true, false, false, false, false
        ));

        // Client with missing SSN (refused)
        clients.add(createClient(
                "C002", "John", "Smith", null,
                LocalDate.of(1990, 7, 22), 8, 1, false, false, true, false, false,
                false, true, false, false, false
        ));

        // Client with approximate DOB
        clients.add(createClient(
                "C003", "Maria", "Garcia", "987654321",
                LocalDate.of(1975, 1, 1), 1, 2, true, false, false, false, false,
                true, false, false, false, false
        ));

        // VAWA-protected client (DV survivor)
        clients.add(createClient(
                "C004", "Protected", "Client", "111223333",
                LocalDate.of(1988, 5, 10), 1, 1, false, false, false, false, true,
                true, false, false, false, false
        ));

        // Client with multiple races
        clients.add(createClient(
                "C005", "Alex", "Johnson", "555667777",
                LocalDate.of(1992, 11, 30), 1, 1, true, false, true, false, false,
                false, false, true, false, false
        ));

        // Veteran client
        clients.add(createClient(
                "C006", "Robert", "Williams", "222334444",
                LocalDate.of(1970, 8, 15), 1, 1, false, false, false, false, true,
                true, false, false, false, false
        ));

        // Client with data quality issues (partial name)
        clients.add(createClient(
                "C007", "Unknown", null, null,
                LocalDate.of(1980, 1, 1), 9, 2, false, false, false, false, false,
                false, false, false, false, true
        ));

        // Youth under 18
        clients.add(createClient(
                "C008", "Young", "Person", "333445555",
                LocalDate.of(2010, 4, 20), 1, 1, false, true, false, false, false,
                false, true, false, false, false
        ));

        return clients;
    }

    /**
     * Generate projects across different types
     */
    public static List<Map<String, Object>> generateProjects() {
        List<Map<String, Object>> projects = new ArrayList<>();

        // Emergency Shelter (ES)
        projects.add(createProject("P001", "O001", "Main Street Shelter", 1));

        // Transitional Housing (TH)
        projects.add(createProject("P002", "O001", "Recovery House", 2));

        // Permanent Supportive Housing (PSH)
        projects.add(createProject("P003", "O002", "Supportive Apartments", 3));

        // Rapid Re-Housing (RRH)
        projects.add(createProject("P004", "O002", "Quick Housing Program", 13));

        // Street Outreach (SO)
        projects.add(createProject("P005", "O001", "Street Outreach Team", 4));

        // Safe Haven
        projects.add(createProject("P006", "O003", "Safe Haven Program", 8));

        return projects;
    }

    /**
     * Generate enrollments with edge cases
     */
    public static List<Map<String, Object>> generateEnrollments() {
        List<Map<String, Object>> enrollments = new ArrayList<>();

        // Standard enrollment
        enrollments.add(createEnrollment(
                "E001", "C001", "P001", "H001",
                LocalDate.of(2024, 1, 15), null, 1, 1, 116
        ));

        // Enrollment with move-in (PSH)
        enrollments.add(createEnrollment(
                "E002", "C002", "P003", "H002",
                LocalDate.of(2024, 2, 1), LocalDate.of(2024, 3, 1), 1, 1, 116
        ));

        // Overlapping enrollments (same client, different project)
        enrollments.add(createEnrollment(
                "E003", "C003", "P001", "H003",
                LocalDate.of(2024, 1, 10), null, 1, 1, 101
        ));
        enrollments.add(createEnrollment(
                "E004", "C003", "P005", "H003",  // Same household, SO enrollment
                LocalDate.of(2024, 1, 20), null, 1, 0, 101
        ));

        // VAWA-protected enrollment
        enrollments.add(createEnrollment(
                "E005", "C004", "P006", "H005",
                LocalDate.of(2024, 3, 1), null, 1, 1, 116
        ));

        // Family enrollment (HoH + children)
        enrollments.add(createEnrollment(
                "E006", "C005", "P002", "H006",
                LocalDate.of(2024, 1, 5), null, 1, 1, 118
        ));
        enrollments.add(createEnrollment(
                "E007", "C008", "P002", "H006",  // Child in same household
                LocalDate.of(2024, 1, 5), null, 2, 0, 118
        ));

        // Veteran enrollment
        enrollments.add(createEnrollment(
                "E008", "C006", "P003", "H008",
                LocalDate.of(2024, 2, 15), LocalDate.of(2024, 3, 15), 1, 1, 101
        ));

        return enrollments;
    }

    /**
     * Generate exits
     */
    public static List<Map<String, Object>> generateExits() {
        List<Map<String, Object>> exits = new ArrayList<>();

        // Exit to permanent housing
        exits.add(createExit("X001", "E001", "C001",
                LocalDate.of(2024, 6, 30), 410));

        // Exit to transitional housing
        exits.add(createExit("X002", "E003", "C003",
                LocalDate.of(2024, 4, 15), 312));

        // Exit - unknown destination
        exits.add(createExit("X003", "E006", "C005",
                LocalDate.of(2024, 8, 1), 30));

        return exits;
    }

    /**
     * Generate services
     */
    public static List<Map<String, Object>> generateServices() {
        List<Map<String, Object>> services = new ArrayList<>();

        // VAWA-protected service (should be filtered in some reports)
        services.add(createService("S001", "E005", "C004",
                LocalDate.of(2024, 3, 5), 1, 1));

        // Standard services
        services.add(createService("S002", "E001", "C001",
                LocalDate.of(2024, 1, 20), 1, 6));

        services.add(createService("S003", "E002", "C002",
                LocalDate.of(2024, 2, 10), 1, 3));

        services.add(createService("S004", "E008", "C006",
                LocalDate.of(2024, 2, 20), 1, 12));

        return services;
    }

    // Factory methods

    private static Map<String, Object> createClient(
            String personalId, String firstName, String lastName, String ssn,
            LocalDate dob, Integer ssnDQ, Integer dobDQ,
            Boolean amInd, Boolean asian, Boolean black, Boolean nativeHI, Boolean white,
            Boolean woman, Boolean man, Boolean nonBinary, Boolean transgender, Boolean genderNone) {

        Map<String, Object> client = new HashMap<>();
        client.put("PersonalID", personalId);
        client.put("FirstName", firstName);
        client.put("LastName", lastName);
        client.put("SSN", ssn);
        client.put("SSNDataQuality", ssnDQ);
        client.put("DOB", dob);
        client.put("DOBDataQuality", dobDQ);
        client.put("AmIndAKNative", amInd);
        client.put("Asian", asian);
        client.put("BlackAfAmerican", black);
        client.put("NativeHIPacific", nativeHI);
        client.put("White", white);
        client.put("RaceNone", false);
        client.put("Woman", woman);
        client.put("Man", man);
        client.put("NonBinary", nonBinary);
        client.put("Transgender", transgender);
        client.put("GenderNone", genderNone);
        return client;
    }

    private static Map<String, Object> createProject(
            String projectId, String orgId, String projectName, Integer projectType) {

        Map<String, Object> project = new HashMap<>();
        project.put("ProjectID", projectId);
        project.put("OrganizationID", orgId);
        project.put("ProjectName", projectName);
        project.put("ProjectType", projectType);
        return project;
    }

    private static Map<String, Object> createEnrollment(
            String enrollmentId, String personalId, String projectId, String householdId,
            LocalDate entryDate, LocalDate moveInDate, Integer relationshipToHoH,
            Integer disablingCondition, Integer livingSituation) {

        Map<String, Object> enrollment = new HashMap<>();
        enrollment.put("EnrollmentID", enrollmentId);
        enrollment.put("PersonalID", personalId);
        enrollment.put("ProjectID", projectId);
        enrollment.put("HouseholdID", householdId);
        enrollment.put("EntryDate", entryDate);
        enrollment.put("MoveInDate", moveInDate);
        enrollment.put("RelationshipToHoH", relationshipToHoH);
        enrollment.put("DisablingCondition", disablingCondition);
        enrollment.put("LivingSituation", livingSituation);
        return enrollment;
    }

    private static Map<String, Object> createExit(
            String exitId, String enrollmentId, String personalId,
            LocalDate exitDate, Integer destination) {

        Map<String, Object> exit = new HashMap<>();
        exit.put("ExitID", exitId);
        exit.put("EnrollmentID", enrollmentId);
        exit.put("PersonalID", personalId);
        exit.put("ExitDate", exitDate);
        exit.put("Destination", destination);
        return exit;
    }

    private static Map<String, Object> createService(
            String servicesId, String enrollmentId, String personalId,
            LocalDate dateProvided, Integer recordType, Integer typeProvided) {

        Map<String, Object> service = new HashMap<>();
        service.put("ServicesID", servicesId);
        service.put("EnrollmentID", enrollmentId);
        service.put("PersonalID", personalId);
        service.put("DateProvided", dateProvided);
        service.put("RecordType", recordType);
        service.put("TypeProvided", typeProvided);
        return service;
    }

    /**
     * Edge case: Client with invalid SSN
     */
    public static Map<String, Object> createClientWithInvalidSSN() {
        return createClient(
                "C999", "Test", "Invalid", "000000000",  // Invalid SSN
                LocalDate.of(1985, 1, 1), 1, 1,
                false, true, false, false, false,
                true, false, false, false, false
        );
    }

    /**
     * Edge case: Enrollment with exit date before entry date
     */
    public static Map<String, Object> createEnrollmentWithInvalidDates() {
        return createEnrollment(
                "E999", "C001", "P001", "H999",
                LocalDate.of(2024, 6, 1),  // Entry
                LocalDate.of(2024, 5, 1),  // Move-in before entry!
                1, 1, 116
        );
    }

    /**
     * Edge case: Exit referencing non-existent enrollment
     */
    public static Map<String, Object> createOrphanExit() {
        return createExit(
                "X999", "E_NONEXISTENT", "C001",
                LocalDate.of(2024, 12, 31), 410
        );
    }
}
