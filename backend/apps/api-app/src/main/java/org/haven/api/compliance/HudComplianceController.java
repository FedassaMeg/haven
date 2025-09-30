package org.haven.api.compliance;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.haven.reporting.application.services.HudComplianceMatrixService;
import org.haven.reporting.application.services.HudComplianceMatrixService.*;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * HUD Compliance API Controller
 * Exposes HUD compliance matrix data and validation endpoints
 */
@RestController
@RequestMapping("/api/v1/compliance")
@Tag(name = "HUD Compliance", description = "HUD compliance matrix and validation endpoints")
public class HudComplianceController {
    
    private final HudComplianceMatrixService complianceMatrixService;
    
    public HudComplianceController(HudComplianceMatrixService complianceMatrixService) {
        this.complianceMatrixService = complianceMatrixService;
    }
    
    @Operation(
        summary = "Get HUD compliance matrix",
        description = "Returns the complete HUD compliance matrix showing coverage across domain/API/UI"
    )
    @GetMapping("/matrix")
    public ResponseEntity<HudComplianceMatrix> getComplianceMatrix() {
        try {
            HudComplianceMatrix matrix = complianceMatrixService.generateComplianceMatrix();
            return ResponseEntity.ok(matrix);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(
        summary = "Export compliance matrix as YAML",
        description = "Exports the HUD compliance matrix in YAML format"
    )
    @GetMapping(value = "/matrix/export/yaml", produces = "application/x-yaml")
    public ResponseEntity<String> exportMatrixAsYaml() {
        try {
            HudComplianceMatrix matrix = complianceMatrixService.generateComplianceMatrix();
            String yamlContent = complianceMatrixService.exportAsYaml(matrix);
            
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=hud-compliance-matrix.yaml");
            
            return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.parseMediaType("application/x-yaml"))
                .body(yamlContent);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(
        summary = "Export compliance matrix as JSON",
        description = "Exports the HUD compliance matrix in JSON format"
    )
    @GetMapping(value = "/matrix/export/json", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<String> exportMatrixAsJson() {
        try {
            HudComplianceMatrix matrix = complianceMatrixService.generateComplianceMatrix();
            String jsonContent = complianceMatrixService.exportAsJson(matrix);
            
            HttpHeaders headers = new HttpHeaders();
            headers.add(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=hud-compliance-matrix.json");
            
            return ResponseEntity.ok()
                .headers(headers)
                .contentType(MediaType.APPLICATION_JSON)
                .body(jsonContent);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(
        summary = "Validate compliance matrix",
        description = "Validates that all mandatory HUD elements have implementation entries"
    )
    @GetMapping("/matrix/validate")
    public ResponseEntity<MatrixValidationResult> validateMatrix() {
        try {
            HudComplianceMatrix matrix = complianceMatrixService.generateComplianceMatrix();
            MatrixValidationResult validation = complianceMatrixService.validateMatrix(matrix);
            
            HttpStatus status = validation.passed() ? HttpStatus.OK : HttpStatus.UNPROCESSABLE_ENTITY;
            return ResponseEntity.status(status).body(validation);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(
        summary = "Get compliance summary",
        description = "Returns a summary of compliance status by category"
    )
    @GetMapping("/summary")
    public ResponseEntity<ComplianceSummaryResponse> getComplianceSummary() {
        try {
            HudComplianceMatrix matrix = complianceMatrixService.generateComplianceMatrix();
            ComplianceSummary summary = matrix.summary();
            
            ComplianceSummaryResponse response = new ComplianceSummaryResponse(
                matrix.overallComplianceScore(),
                summary.totalElements(),
                summary.fullyImplemented(),
                summary.partiallyImplemented(),
                summary.notImplemented(),
                summary.byCategory().entrySet().stream()
                    .collect(java.util.stream.Collectors.toMap(
                        entry -> entry.getKey().name(),
                        entry -> new CategorySummary(
                            entry.getKey().getDisplayName(),
                            entry.getValue().totalElements(),
                            entry.getValue().implementedElements(),
                            entry.getValue().compliancePercentage()
                        )
                    )),
                matrix.generatedAt()
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(
        summary = "Get HUD elements by category",
        description = "Returns HUD elements filtered by category"
    )
    @GetMapping("/elements")
    public ResponseEntity<List<HudDataElement>> getHudElements(
            @Parameter(description = "Filter by HUD element category")
            @RequestParam(required = false) HudElementCategory category,
            @Parameter(description = "Filter by mandatory status")
            @RequestParam(required = false) Boolean mandatory,
            @Parameter(description = "Filter by implementation status")
            @RequestParam(required = false) Boolean implemented) {
        
        try {
            HudComplianceMatrix matrix = complianceMatrixService.generateComplianceMatrix();
            List<HudDataElement> elements = matrix.hudElements();
            
            // Apply filters
            if (category != null) {
                elements = elements.stream()
                    .filter(element -> element.category() == category)
                    .toList();
            }
            
            if (mandatory != null) {
                elements = elements.stream()
                    .filter(element -> element.mandatory() == mandatory)
                    .toList();
            }
            
            if (implemented != null) {
                elements = elements.stream()
                    .filter(element -> {
                        boolean isImplemented = element.domainImplementation().implemented() &&
                                              element.apiImplementation().implemented() &&
                                              element.uiImplementation().implemented();
                        return isImplemented == implemented;
                    })
                    .toList();
            }
            
            return ResponseEntity.ok(elements);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    @Operation(
        summary = "Get element coverage details",
        description = "Returns detailed coverage information for a specific HUD element"
    )
    @GetMapping("/elements/{hudId}/coverage")
    public ResponseEntity<ElementCoverageResponse> getElementCoverage(
            @Parameter(description = "HUD element ID (e.g., '3.01', '3.06')")
            @PathVariable String hudId) {
        
        try {
            HudComplianceMatrix matrix = complianceMatrixService.generateComplianceMatrix();
            
            HudDataElement element = matrix.hudElements().stream()
                .filter(e -> e.hudId().equals(hudId))
                .findFirst()
                .orElse(null);
            
            if (element == null) {
                return ResponseEntity.notFound().build();
            }
            
            ElementCoverageResponse response = new ElementCoverageResponse(
                element.hudId(),
                element.name(),
                element.description(),
                element.mandatory(),
                element.category().getDisplayName(),
                element.owningAggregate(),
                new CoverageDetail(
                    "Domain",
                    element.domainImplementation().implemented(),
                    element.domainImplementation().aggregateClass(),
                    element.domainImplementation().method()
                ),
                new CoverageDetail(
                    "API",
                    element.apiImplementation().implemented(),
                    element.apiImplementation().route(),
                    element.apiImplementation().httpMethod() + " " + element.apiImplementation().fieldName()
                ),
                new CoverageDetail(
                    "UI",
                    element.uiImplementation().implemented(),
                    element.uiImplementation().componentName(),
                    element.uiImplementation().fieldName()
                ),
                element.notes()
            );
            
            return ResponseEntity.ok(response);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }
    
    // Response DTOs
    public record ComplianceSummaryResponse(
        double overallScore,
        long totalElements,
        long fullyImplemented,
        long partiallyImplemented,
        long notImplemented,
        Map<String, CategorySummary> categories,
        java.util.Date lastUpdated
    ) {}
    
    public record CategorySummary(
        String displayName,
        long totalElements,
        long implementedElements,
        double compliancePercentage
    ) {}
    
    public record ElementCoverageResponse(
        String hudId,
        String name,
        String description,
        boolean mandatory,
        String category,
        String owningAggregate,
        CoverageDetail domainCoverage,
        CoverageDetail apiCoverage,
        CoverageDetail uiCoverage,
        String notes
    ) {}
    
    public record CoverageDetail(
        String layer,
        boolean implemented,
        String location,
        String details
    ) {}
}