package org.haven.clientprofile.application.services;

import org.haven.clientprofile.domain.Client;
import org.haven.clientprofile.domain.ClientId;
import org.haven.clientprofile.domain.ClientRepository;
import org.haven.clientprofile.domain.pii.PIIAccessContext;
import org.haven.clientprofile.domain.privacy.*;
import org.haven.clientprofile.domain.privacy.UniversalDataElementPrivacyPolicy.DataAccessPurpose;
import org.haven.clientprofile.domain.privacy.RacePrivacyControl.RaceRedactionStrategy;
import org.haven.shared.vo.hmis.HmisEthnicity;
import org.haven.shared.vo.hmis.HmisEthnicity.EthnicityPrecision;
import org.haven.shared.vo.hmis.HmisRace;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Service for managing demographic data with privacy controls.
 * Implements HMIS 2024 standards for Race and Ethnicity with full privacy support.
 */
@Service
@Transactional
public class DemographicPrivacyService {
    
    private final ClientRepository clientRepository;
    private final JdbcTemplate jdbcTemplate;
    private final UniversalDataElementPrivacyPolicy privacyPolicy;
    
    public DemographicPrivacyService(ClientRepository clientRepository, JdbcTemplate jdbcTemplate) {
        this.clientRepository = clientRepository;
        this.jdbcTemplate = jdbcTemplate;
        this.privacyPolicy = new UniversalDataElementPrivacyPolicy();
    }
    
    /**
     * Updates client demographics with privacy controls
     */
    public void updateClientDemographics(UUID clientId, 
                                        Set<HmisRace> races, 
                                        HmisEthnicity ethnicity,
                                        RaceRedactionStrategy defaultRaceStrategy,
                                        EthnicityPrecision defaultEthnicityPrecision,
                                        PIIAccessContext context) {
        Client client = clientRepository.findById(new ClientId(clientId))
            .orElseThrow(() -> new IllegalArgumentException("Client not found: " + clientId));
        
        // Update demographics
        client.updateRace(races, defaultRaceStrategy);
        client.updateEthnicity(ethnicity, defaultEthnicityPrecision);
        
        // Save to database
        clientRepository.save(client);
        
        // Update database tables
        updateDemographicsInDatabase(clientId, races, ethnicity, defaultRaceStrategy, defaultEthnicityPrecision);
        
        // Log the access
        logDemographicAccess(clientId, context, DataAccessPurpose.DIRECT_SERVICE, true, true);
    }
    
    /**
     * Gets client demographics with privacy controls applied
     */
    public DemographicData getClientDemographics(UUID clientId, 
                                                PIIAccessContext context, 
                                                DataAccessPurpose purpose) {
        Client client = clientRepository.findById(new ClientId(clientId))
            .orElseThrow(() -> new IllegalArgumentException("Client not found: " + clientId));
        
        // Get privacy-controlled data
        Set<HmisRace> redactedRaces = client.getRaceWithPrivacy(context, purpose);
        HmisEthnicity redactedEthnicity = client.getEthnicityWithPrivacy(context, purpose);
        
        // Log the access
        boolean raceAccessed = !redactedRaces.isEmpty();
        boolean ethnicityAccessed = redactedEthnicity != HmisEthnicity.DATA_NOT_COLLECTED;
        logDemographicAccess(clientId, context, purpose, raceAccessed, ethnicityAccessed);
        
        return new DemographicData(clientId, redactedRaces, redactedEthnicity, 
                                  client.getDemographicProjection(context, purpose));
    }
    
    /**
     * Sets privacy override for a specific client and purpose
     */
    public void setPrivacyOverride(UUID clientId, 
                                  DataAccessPurpose purpose,
                                  RaceRedactionStrategy raceStrategy,
                                  EthnicityPrecision ethnicityPrecision,
                                  String reason,
                                  PIIAccessContext context) {
        // Check authorization
        if (!context.hasRole("PRIVACY_OFFICER") && !context.hasRole("ADMINISTRATOR")) {
            throw new SecurityException("Insufficient privileges to set privacy overrides");
        }
        
        // Insert or update override
        String sql = """
            INSERT INTO demographic_privacy_overrides 
            (client_id, purpose, race_strategy, ethnicity_precision, override_reason, authorized_by, created_by)
            VALUES (?, ?::data_access_purpose, ?::race_redaction_strategy, ?::ethnicity_precision, ?, ?, ?)
            ON CONFLICT (client_id, purpose) 
            DO UPDATE SET 
                race_strategy = EXCLUDED.race_strategy,
                ethnicity_precision = EXCLUDED.ethnicity_precision,
                override_reason = EXCLUDED.override_reason,
                authorized_by = EXCLUDED.authorized_by,
                updated_at = CURRENT_TIMESTAMP
            """;
        
        jdbcTemplate.update(sql, clientId, purpose.name(), 
                          raceStrategy.name(), ethnicityPrecision.name(), 
                          reason, context.getUserId(), context.getUserId());
    }
    
    /**
     * Gets effective privacy controls for a client
     */
    public PrivacyControls getEffectivePrivacyControls(UUID clientId, 
                                                      DataAccessPurpose purpose,
                                                      PIIAccessContext context) {
        String sql = "SELECT * FROM get_effective_privacy_controls(?, ?::data_access_purpose, ?)";
        
        return jdbcTemplate.queryForObject(sql, 
            (rs, rowNum) -> new PrivacyControls(
                RaceRedactionStrategy.valueOf(rs.getString("race_strategy")),
                EthnicityPrecision.valueOf(rs.getString("ethnicity_precision")),
                rs.getString("source")
            ),
            clientId, purpose.name(), context.getUserId()
        );
    }
    
    /**
     * Gets demographic access history for a client
     */
    public List<DemographicAccessLog> getAccessHistory(UUID clientId, PIIAccessContext context) {
        // Check authorization
        if (!context.hasRole("AUDITOR") && !context.hasRole("PRIVACY_OFFICER") && !context.hasRole("ADMINISTRATOR")) {
            throw new SecurityException("Insufficient privileges to view access history");
        }
        
        String sql = """
            SELECT dal.*, u.username 
            FROM demographic_access_log dal
            JOIN users u ON dal.accessed_by = u.id
            WHERE dal.client_id = ?
            ORDER BY dal.accessed_at DESC
            LIMIT 100
            """;
        
        return jdbcTemplate.query(sql, 
            (rs, rowNum) -> new DemographicAccessLog(
                rs.getObject("id", UUID.class),
                rs.getObject("client_id", UUID.class),
                rs.getObject("accessed_by", UUID.class),
                rs.getString("username"),
                DataAccessPurpose.valueOf(rs.getString("access_purpose")),
                rs.getBoolean("race_accessed"),
                rs.getString("race_strategy_applied") != null ? 
                    RaceRedactionStrategy.valueOf(rs.getString("race_strategy_applied")) : null,
                rs.getBoolean("ethnicity_accessed"),
                rs.getString("ethnicity_precision_applied") != null ?
                    EthnicityPrecision.valueOf(rs.getString("ethnicity_precision_applied")) : null,
                rs.getTimestamp("accessed_at").toInstant()
            ),
            clientId
        );
    }
    
    /**
     * Updates demographics in database tables
     */
    private void updateDemographicsInDatabase(UUID clientId, 
                                             Set<HmisRace> races,
                                             HmisEthnicity ethnicity,
                                             RaceRedactionStrategy raceStrategy,
                                             EthnicityPrecision ethnicityPrecision) {
        String sql = """
            UPDATE client_demographics 
            SET hmis_race = ?,
                hmis_ethnicity = ?,
                default_race_strategy = ?::race_redaction_strategy,
                default_ethnicity_precision = ?::ethnicity_precision,
                updated_at = CURRENT_TIMESTAMP
            WHERE client_id = ?
            """;
        
        String[] raceArray = races.stream()
            .map(HmisRace::name)
            .toArray(String[]::new);
        
        jdbcTemplate.update(sql, 
            raceArray, 
            ethnicity.name(),
            raceStrategy.name(),
            ethnicityPrecision.name(),
            clientId
        );
    }
    
    /**
     * Logs demographic data access
     */
    private void logDemographicAccess(UUID clientId, 
                                     PIIAccessContext context,
                                     DataAccessPurpose purpose,
                                     boolean raceAccessed,
                                     boolean ethnicityAccessed) {
        // Get effective controls
        PrivacyControls controls = getEffectivePrivacyControls(clientId, purpose, context);
        
        String sql = "SELECT log_demographic_access(?, ?, ?::data_access_purpose, ?, ?::race_redaction_strategy, ?, ?::ethnicity_precision, ?::jsonb)";
        
        Map<String, Object> contextMap = new HashMap<>();
        contextMap.put("sessionId", context.getSessionId());
        contextMap.put("ipAddress", context.getIpAddress());
        contextMap.put("justification", context.getBusinessJustification());
        
        jdbcTemplate.queryForObject(sql, UUID.class,
            clientId,
            context.getUserId(),
            purpose.name(),
            raceAccessed,
            controls.raceStrategy().name(),
            ethnicityAccessed,
            controls.ethnicityPrecision().name(),
            contextMap.toString()
        );
    }
    
    /**
     * Data class for demographic information with privacy controls
     */
    public record DemographicData(
        UUID clientId,
        Set<HmisRace> races,
        HmisEthnicity ethnicity,
        Map<String, Object> projection
    ) {}
    
    /**
     * Data class for privacy controls
     */
    public record PrivacyControls(
        RaceRedactionStrategy raceStrategy,
        EthnicityPrecision ethnicityPrecision,
        String source
    ) {}
    
    /**
     * Data class for access log entries
     */
    public record DemographicAccessLog(
        UUID id,
        UUID clientId,
        UUID accessedBy,
        String username,
        DataAccessPurpose purpose,
        boolean raceAccessed,
        RaceRedactionStrategy raceStrategy,
        boolean ethnicityAccessed,
        EthnicityPrecision ethnicityPrecision,
        Instant accessedAt
    ) {}
}