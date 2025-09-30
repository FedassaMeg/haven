package org.haven.reporting.application.services;

import org.haven.reporting.application.services.HudComplianceMatrixService.*;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * CLI application to generate HUD compliance matrix files
 * Used by Gradle build process to create machine-readable artifacts
 */
public class ComplianceMatrixGenerator {
    
    public static void main(String[] args) {
        try {
            HudComplianceMatrixService service = new HudComplianceMatrixService();
            HudComplianceMatrix matrix = service.generateComplianceMatrix();
            
            // Create output directory
            File outputDir = new File("build/compliance");
            outputDir.mkdirs();
            
            // Generate JSON file
            String json = service.exportAsJson(matrix);
            writeToFile(new File(outputDir, "hud-compliance-matrix.json"), json);
            System.out.println("📄 Generated: build/compliance/hud-compliance-matrix.json");
            
            // Generate YAML file
            String yaml = service.exportAsYaml(matrix);
            writeToFile(new File(outputDir, "hud-compliance-matrix.yaml"), yaml);
            System.out.println("📄 Generated: build/compliance/hud-compliance-matrix.yaml");
            
            // Generate summary report
            generateSummaryReport(matrix, outputDir);
            
            System.out.println("✅ HUD compliance matrix generation completed successfully");
            System.out.printf("📊 Overall compliance score: %.1f%%\n", matrix.overallComplianceScore());
            
        } catch (Exception e) {
            System.err.println("❌ Failed to generate HUD compliance matrix: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
    }
    
    private static void writeToFile(File file, String content) throws IOException {
        try (FileWriter writer = new FileWriter(file)) {
            writer.write(content);
        }
    }
    
    private static void generateSummaryReport(HudComplianceMatrix matrix, File outputDir) throws IOException {
        StringBuilder report = new StringBuilder();
        report.append("# HUD Compliance Matrix Summary\n\n");
        report.append("Generated: ").append(matrix.generatedAt()).append("\n");
        report.append("Version: ").append(matrix.version()).append("\n\n");
        
        report.append("## Overall Compliance\n");
        report.append("Score: ").append(String.format("%.1f%%", matrix.overallComplianceScore())).append("\n\n");
        
        ComplianceSummary summary = matrix.summary();
        report.append("## Summary by Implementation Status\n");
        report.append("- Total Elements: ").append(summary.totalElements()).append("\n");
        report.append("- Fully Implemented: ").append(summary.fullyImplemented()).append("\n");
        report.append("- Partially Implemented: ").append(summary.partiallyImplemented()).append("\n");
        report.append("- Not Implemented: ").append(summary.notImplemented()).append("\n\n");
        
        report.append("## Summary by Category\n");
        for (var entry : summary.byCategory().entrySet()) {
            var category = entry.getKey();
            var categoryData = entry.getValue();
            report.append("### ").append(category.getDisplayName()).append("\n");
            report.append("- Compliance: ").append(String.format("%.1f%%", categoryData.compliancePercentage())).append("\n");
            report.append("- Implemented: ").append(categoryData.implementedElements())
                  .append("/").append(categoryData.totalElements()).append("\n\n");
        }
        
        report.append("## Missing Implementations\n");
        for (HudDataElement element : matrix.hudElements()) {
            if (!element.domainImplementation().implemented() || 
                !element.apiImplementation().implemented() || 
                !element.uiImplementation().implemented()) {
                
                report.append("### ").append(element.hudId()).append(" - ").append(element.name()).append("\n");
                if (!element.domainImplementation().implemented()) {
                    report.append("- ❌ Domain implementation missing\n");
                }
                if (!element.apiImplementation().implemented()) {
                    report.append("- ❌ API implementation missing\n");
                }
                if (!element.uiImplementation().implemented()) {
                    report.append("- ❌ UI implementation missing\n");
                }
                report.append("\n");
            }
        }
        
        writeToFile(new File(outputDir, "compliance-summary.md"), report.toString());
        System.out.println("📄 Generated: build/compliance/compliance-summary.md");
    }
}