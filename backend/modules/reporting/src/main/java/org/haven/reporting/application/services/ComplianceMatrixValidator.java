package org.haven.reporting.application.services;

import org.haven.reporting.application.services.HudComplianceMatrixService.*;

/**
 * CLI application to validate HUD compliance matrix
 * Used as a build gate to ensure 100% coverage of mandatory elements
 */
public class ComplianceMatrixValidator {
    
    public static void main(String[] args) {
        try {
            HudComplianceMatrixService service = new HudComplianceMatrixService();
            HudComplianceMatrix matrix = service.generateComplianceMatrix();
            MatrixValidationResult validation = service.validateMatrix(matrix);
            
            System.out.println("🔍 Validating HUD compliance matrix...");
            System.out.printf("📊 Overall compliance score: %.1f%%\n", matrix.overallComplianceScore());
            
            // Check for violations (mandatory elements without domain/API implementation)
            if (!validation.violations().isEmpty()) {
                System.err.println("\n❌ COMPLIANCE VALIDATION FAILED");
                System.err.println("The following mandatory HUD elements are missing required implementations:\n");
                
                for (String violation : validation.violations()) {
                    System.err.println("  • " + violation);
                }
                
                System.err.println("\n💡 To fix these violations:");
                System.err.println("  1. Implement missing domain methods in aggregate classes");
                System.err.println("  2. Add corresponding API endpoints in controllers");
                System.err.println("  3. Update the compliance matrix service with implementation status");
                System.err.println("\n🚫 Build gate: Cannot proceed with missing mandatory implementations");
                
                System.exit(1);
            }
            
            // Check for warnings (mandatory elements without UI implementation)
            if (!validation.warnings().isEmpty()) {
                System.out.println("\n⚠️  COMPLIANCE WARNINGS");
                System.out.println("The following mandatory HUD elements are missing UI implementations:\n");
                
                for (String warning : validation.warnings()) {
                    System.out.println("  • " + warning);
                }
                
                System.out.println("\n💡 These warnings do not block the build but should be addressed");
            }
            
            // Check overall compliance threshold
            double minimumThreshold = 85.0; // 85% minimum for production readiness
            if (matrix.overallComplianceScore() < minimumThreshold) {
                System.err.printf("\n❌ COMPLIANCE THRESHOLD NOT MET\n");
                System.err.printf("Current compliance: %.1f%% (minimum required: %.1f%%)\n", 
                    matrix.overallComplianceScore(), minimumThreshold);
                System.err.println("\n🚫 Build gate: Compliance score too low for production deployment");
                System.exit(1);
            }
            
            // Success!
            System.out.println("\n✅ HUD COMPLIANCE VALIDATION PASSED");
            System.out.printf("✅ All mandatory elements have domain and API implementations\n");
            System.out.printf("✅ Compliance score (%.1f%%) meets minimum threshold (%.1f%%)\n", 
                matrix.overallComplianceScore(), minimumThreshold);
            
            // Print compliance summary
            ComplianceSummary summary = matrix.summary();
            System.out.println("\n📊 Compliance Summary:");
            System.out.printf("  • Total HUD Elements: %d\n", summary.totalElements());
            System.out.printf("  • Fully Implemented: %d (%.1f%%)\n", 
                summary.fullyImplemented(), 
                (double) summary.fullyImplemented() / summary.totalElements() * 100);
            System.out.printf("  • Partially Implemented: %d (%.1f%%)\n", 
                summary.partiallyImplemented(),
                (double) summary.partiallyImplemented() / summary.totalElements() * 100);
            System.out.printf("  • Not Implemented: %d (%.1f%%)\n", 
                summary.notImplemented(),
                (double) summary.notImplemented() / summary.totalElements() * 100);
            
            System.out.println("\n🎯 Ready for production deployment!");
            
        } catch (Exception e) {
            System.err.println("❌ Failed to validate HUD compliance matrix: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
}