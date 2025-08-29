package org.haven.clientprofile.interfaces.rest;

import org.haven.clientprofile.application.services.DemographicPrivacyService;
import org.haven.clientprofile.application.services.DemographicPrivacyService.*;
import org.haven.clientprofile.domain.pii.PIIAccessContext;
import org.haven.clientprofile.domain.privacy.RacePrivacyControl.RaceRedactionStrategy;
import org.haven.clientprofile.domain.privacy.UniversalDataElementPrivacyPolicy.DataAccessPurpose;
import org.haven.shared.vo.hmis.HmisEthnicity;
import org.haven.shared.vo.hmis.HmisEthnicity.EthnicityPrecision;
import org.haven.shared.vo.hmis.HmisRace;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.*;
import java.util.stream.Collectors;

/**
 * REST API for managing demographic data with privacy controls.
 * Implements HMIS 2024 Universal Data Element standards.
 */
@RestController
@RequestMapping("/api/v1/clients/{clientId}/demographics")
public class DemographicPrivacyController {
    
    private final DemographicPrivacyService demographicService;
    
    public DemographicPrivacyController(DemographicPrivacyService demographicService) {
        this.demographicService = demographicService;
    }
    
    /**
     * Get client demographics with privacy controls applied
     */
    @GetMapping
    public ResponseEntity<DemographicResponse> getClientDemographics(
            @PathVariable UUID clientId,
            @RequestParam(required = false, defaultValue = "DIRECT_SERVICE") DataAccessPurpose purpose,
            @RequestParam(required = false) String justification,
            HttpServletRequest request) {
        
        PIIAccessContext context = createAccessContext(justification, request);
        DemographicData data = demographicService.getClientDemographics(clientId, context, purpose);
        
        return ResponseEntity.ok(new DemographicResponse(
            data.clientId(),
            data.races().stream().map(HmisRace::name).collect(Collectors.toSet()),
            data.ethnicity().name(),
            data.projection()
        ));
    }
    
    /**
     * Update client demographics
     */
    @PutMapping
    public ResponseEntity<Void> updateClientDemographics(
            @PathVariable UUID clientId,
            @Valid @RequestBody DemographicUpdateRequest request,
            HttpServletRequest httpRequest) {
        
        PIIAccessContext context = createAccessContext(request.justification(), httpRequest);
        
        Set<HmisRace> races = request.races().stream()
            .map(HmisRace::valueOf)
            .collect(Collectors.toSet());
        
        HmisEthnicity ethnicity = HmisEthnicity.valueOf(request.ethnicity());
        
        demographicService.updateClientDemographics(
            clientId, 
            races, 
            ethnicity,
            request.defaultRaceStrategy() != null ? request.defaultRaceStrategy() : RaceRedactionStrategy.FULL_DISCLOSURE,
            request.defaultEthnicityPrecision() != null ? request.defaultEthnicityPrecision() : EthnicityPrecision.FULL,
            context
        );
        
        return ResponseEntity.noContent().build();
    }
    
    /**
     * Set privacy override for a client
     */
    @PostMapping("/privacy-override")
    public ResponseEntity<Void> setPrivacyOverride(
            @PathVariable UUID clientId,
            @Valid @RequestBody PrivacyOverrideRequest request,
            HttpServletRequest httpRequest) {
        
        PIIAccessContext context = createAccessContext(request.reason(), httpRequest);
        
        demographicService.setPrivacyOverride(
            clientId,
            request.purpose(),
            request.raceStrategy(),
            request.ethnicityPrecision(),
            request.reason(),
            context
        );
        
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
    
    /**
     * Get effective privacy controls for a client
     */
    @GetMapping("/privacy-controls")
    public ResponseEntity<PrivacyControlsResponse> getEffectivePrivacyControls(
            @PathVariable UUID clientId,
            @RequestParam DataAccessPurpose purpose,
            HttpServletRequest request) {
        
        PIIAccessContext context = createAccessContext(null, request);
        PrivacyControls controls = demographicService.getEffectivePrivacyControls(clientId, purpose, context);
        
        return ResponseEntity.ok(new PrivacyControlsResponse(
            controls.raceStrategy(),
            controls.ethnicityPrecision(),
            controls.source()
        ));
    }
    
    /**
     * Get demographic access history
     */
    @GetMapping("/access-history")
    public ResponseEntity<List<AccessLogResponse>> getAccessHistory(
            @PathVariable UUID clientId,
            HttpServletRequest request) {
        
        PIIAccessContext context = createAccessContext(null, request);
        List<DemographicAccessLog> logs = demographicService.getAccessHistory(clientId, context);
        
        List<AccessLogResponse> response = logs.stream()
            .map(log -> new AccessLogResponse(
                log.id(),
                log.username(),
                log.purpose(),
                log.raceAccessed(),
                log.raceStrategy(),
                log.ethnicityAccessed(),
                log.ethnicityPrecision(),
                log.accessedAt()
            ))
            .collect(Collectors.toList());
        
        return ResponseEntity.ok(response);
    }
    
    /**
     * Create PIIAccessContext from request
     */
    private PIIAccessContext createAccessContext(String justification, HttpServletRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        
        UUID userId = null;
        List<String> roles = new ArrayList<>();
        
        if (auth != null && auth.isAuthenticated()) {
            // Extract user ID from authentication (implementation depends on your auth setup)
            // userId = extractUserId(auth);
            roles = auth.getAuthorities().stream()
                .map(a -> a.getAuthority())
                .collect(Collectors.toList());
        }
        
        return new PIIAccessContext(
            userId,
            roles,
            justification,
            null, // caseId - would be extracted from context
            request.getSession().getId(),
            request.getRemoteAddr()
        );
    }
    
    // Request/Response DTOs
    
    public record DemographicResponse(
        UUID clientId,
        Set<String> races,
        String ethnicity,
        Map<String, Object> privacyProjection
    ) {}
    
    public record DemographicUpdateRequest(
        @NotNull Set<String> races,
        @NotNull String ethnicity,
        RaceRedactionStrategy defaultRaceStrategy,
        EthnicityPrecision defaultEthnicityPrecision,
        String justification
    ) {}
    
    public record PrivacyOverrideRequest(
        @NotNull DataAccessPurpose purpose,
        @NotNull RaceRedactionStrategy raceStrategy,
        @NotNull EthnicityPrecision ethnicityPrecision,
        @NotNull String reason
    ) {}
    
    public record PrivacyControlsResponse(
        RaceRedactionStrategy raceStrategy,
        EthnicityPrecision ethnicityPrecision,
        String source
    ) {}
    
    public record AccessLogResponse(
        UUID id,
        String username,
        DataAccessPurpose purpose,
        boolean raceAccessed,
        RaceRedactionStrategy raceStrategy,
        boolean ethnicityAccessed,
        EthnicityPrecision ethnicityPrecision,
        java.time.Instant accessedAt
    ) {}
}