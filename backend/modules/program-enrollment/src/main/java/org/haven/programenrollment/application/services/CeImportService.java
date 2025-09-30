package org.haven.programenrollment.application.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import org.haven.clientprofile.domain.consent.Consent;
import org.haven.clientprofile.domain.consent.ConsentId;
import org.haven.clientprofile.domain.consent.ConsentRepository;
import org.haven.clientprofile.domain.consent.ConsentStatus;
import org.haven.clientprofile.infrastructure.persistence.ConsentLedgerEntity;
import org.haven.clientprofile.infrastructure.persistence.ConsentLedgerRepository;
import org.haven.programenrollment.application.security.HmisAuditLogger;
import org.haven.programenrollment.domain.ProgramEnrollmentId;
import org.haven.programenrollment.domain.ce.CeAssessmentLevel;
import org.haven.programenrollment.domain.ce.CeAssessmentType;
import org.haven.programenrollment.domain.ce.CeEventResult;
import org.haven.programenrollment.domain.ce.CeEventStatus;
import org.haven.programenrollment.domain.ce.CeEventType;
import org.haven.programenrollment.domain.ce.CeHashAlgorithm;
import org.haven.programenrollment.domain.ce.CePrioritizationStatus;
import org.haven.programenrollment.domain.ce.CeShareScope;
import org.haven.programenrollment.infrastructure.persistence.JpaCeImportJobEntity;
import org.haven.programenrollment.infrastructure.persistence.JpaCeImportJobRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.math.BigDecimal;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class CeImportService {

    private static final String DEFAULT_SOURCE = "CE_IMPORT";

    private final CeAssessmentService assessmentService;
    private final CeEventService eventService;
    private final ConsentRepository consentRepository;
    private final ConsentLedgerRepository consentLedgerRepository;
    private final JpaCeImportJobRepository jobRepository;
    private final ConsentLedgerUpdatePublisher ledgerUpdatePublisher;
    private final HmisAuditLogger auditLogger;
    private final ObjectMapper objectMapper;

    public CeImportService(CeAssessmentService assessmentService,
                           CeEventService eventService,
                           ConsentRepository consentRepository,
                           ConsentLedgerRepository consentLedgerRepository,
                           JpaCeImportJobRepository jobRepository,
                           ConsentLedgerUpdatePublisher ledgerUpdatePublisher,
                           HmisAuditLogger auditLogger,
                           ObjectMapper objectMapper) {
        this.assessmentService = assessmentService;
        this.eventService = eventService;
        this.consentRepository = consentRepository;
        this.consentLedgerRepository = consentLedgerRepository;
        this.jobRepository = jobRepository;
        this.ledgerUpdatePublisher = ledgerUpdatePublisher;
        this.auditLogger = auditLogger;
        this.objectMapper = objectMapper;
    }

    @Transactional
    public CeImportResult importHmisCsv(String csvContent, ImportOptions options) {
        return process("HMIS_CSV", options, () -> parseCsv(csvContent, options.delimiter()));
    }

    @Transactional
    public CeImportResult importHmisXml(String xmlContent, ImportOptions options) {
        return process("HMIS_XML", options, () -> parseXml(xmlContent));
    }

    @Transactional
    public CeImportResult importVendorFeed(String jsonContent, ImportOptions options) {
        return process("VENDOR_FEED", options, () -> parseVendorFeed(jsonContent));
    }

    private CeImportResult process(String format,
                                   ImportOptions options,
                                   ImportSupplier supplier) {
        UUID jobId = UUID.randomUUID();
        JpaCeImportJobEntity job = new JpaCeImportJobEntity(
            jobId,
            options.sourceSystem(),
            format,
            "CREATED",
            options.initiatedBy(),
            options.fileName(),
            0,
            0,
            0,
            0,
            null,
            Instant.now(),
            null
        );
        job.markProcessing();
        jobRepository.save(job);

        List<ImportRecord> records;
        try {
            records = supplier.get();
        } catch (Exception ex) {
            job.markFailed("Unable to parse payload: " + ex.getMessage());
            jobRepository.save(job);
            throw new IllegalArgumentException("Unable to parse import payload", ex);
        }

        int success = 0;
        int failures = 0;
        int warnings = 0;
        StringBuilder errorBuffer = new StringBuilder();

        for (ImportRecord record : records) {
            try {
                handleRecord(record, options);
                success++;
            } catch (Exception ex) {
                failures++;
                errorBuffer.append(String.format("%s (%s): %s", record.recordType(), record.enrollmentId(), ex.getMessage()))
                    .append(System.lineSeparator());
                auditLogger.logUnauthorizedAccess("CE_IMPORT", record.enrollmentId().value(), format, ex.getMessage());
            }

            if (record.warning().isPresent()) {
                warnings++;
                job.appendErrorLog("Warning: " + record.warning().get());
            }
            job.incrementTotals(success > failures, record.warning().isPresent());
        }

        job.markCompleted(success, failures, warnings,
            errorBuffer.length() == 0 ? job.getErrorLog() : errorBuffer.toString());
        jobRepository.save(job);

        return new CeImportResult(jobId, success, failures, warnings);
    }

    private void handleRecord(ImportRecord record, ImportOptions options) {
        validateConsent(record);

        Set<CeShareScope> scopes = record.shareScopes().isEmpty()
            ? Set.of(CeShareScope.COC_COORDINATED_ENTRY)
            : record.shareScopes();

        if (record.recordType() == ImportRecordType.ASSESSMENT) {
            CeAssessmentService.CreateAssessmentCommand command = new CeAssessmentService.CreateAssessmentCommand(
                record.enrollmentId().value(),
                record.clientId(),
                record.assessmentDate().orElseThrow(() -> new IllegalArgumentException("Assessment date missing")),
                record.assessmentType().orElseThrow(() -> new IllegalArgumentException("Assessment type missing")),
                record.assessmentLevel().orElse(null),
                record.toolUsed().orElse(null),
                record.score().map(BigDecimal::valueOf).orElse(null),
                record.prioritizationStatus().orElse(null),
                record.location().orElse(null),
                record.consentId(),
                record.consentLedgerId().orElse(null),
                scopes,
                record.hashAlgorithm(),
                record.encryptionScheme().orElse(null),
                record.encryptionKeyId(),
                record.encryptionMetadata(),
                record.encryptionTags(),
                options.initiatedBy(),
                record.recipientOrganization().orElse(null)
            );

            var assessment = assessmentService.recordAssessment(command);
            publishLedgerUpdate(record, assessment.getPacketId() != null ? assessment.getPacketId().value() : null);
        } else {
            CeEventService.CreateEventCommand command = new CeEventService.CreateEventCommand(
                record.enrollmentId().value(),
                record.clientId(),
                record.eventDate().orElseThrow(() -> new IllegalArgumentException("Event date missing")),
                record.eventType().orElseThrow(() -> new IllegalArgumentException("Event type missing")),
                record.eventResult().orElse(null),
                record.eventStatus().orElseThrow(() -> new IllegalArgumentException("Event status missing")),
                record.referralDestination().orElse(null),
                record.outcomeDate().orElse(null),
                record.consentId(),
                record.consentLedgerId().orElse(null),
                scopes,
                record.hashAlgorithm(),
                record.encryptionScheme().orElse(null),
                record.encryptionKeyId(),
                record.encryptionMetadata(),
                record.encryptionTags(),
                options.initiatedBy()
            );

            var ceEvent = eventService.recordEvent(command);
            publishLedgerUpdate(record, ceEvent.getPacketId() != null ? ceEvent.getPacketId().value() : null);
        }
    }

    private void validateConsent(ImportRecord record) {
        if (!record.consentGranted()) {
            throw new IllegalStateException("Consent flag marked false");
        }

        Consent consent = consentRepository.findById(ConsentId.fromString(record.consentId().toString()))
            .orElseThrow(() -> new IllegalArgumentException("Consent not found: " + record.consentId()));

        if (!consent.isValidForUse()) {
            throw new IllegalStateException("Consent is not active");
        }

        record.consentLedgerId().ifPresent(ledgerId -> {
            ConsentLedgerEntity ledger = consentLedgerRepository.findById(ledgerId)
                .orElseThrow(() -> new IllegalArgumentException("Consent ledger entry not found: " + ledgerId));
            if (ledger.getStatus() != ConsentStatus.GRANTED) {
                throw new IllegalStateException("Consent ledger entry is not granted");
            }
        });
    }

    private void publishLedgerUpdate(ImportRecord record, UUID packetId) {
        String payloadSignature = hash(record);
        ledgerUpdatePublisher.publishPendingUpdate(record.consentId(), packetId, record.sourceSystem(), payloadSignature);
    }

    private List<ImportRecord> parseCsv(String content, char delimiter) throws IOException {
        List<ImportRecord> records = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(new StringReader(content))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return records;
            }
            String[] headers = headerLine.split(Character.toString(delimiter));
            Map<String, Integer> columnIndex = new HashMap<>();
            for (int i = 0; i < headers.length; i++) {
                columnIndex.put(headers[i].trim().toLowerCase(Locale.ROOT), i);
            }
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isBlank()) continue;
                String[] values = line.split(Character.toString(delimiter), -1);
                records.add(mapRow(values, columnIndex, "HMIS_CSV"));
            }
        }
        return records;
    }

    private List<ImportRecord> parseXml(String xmlContent) throws Exception {
        Document document = DocumentBuilderFactory.newInstance()
            .newDocumentBuilder()
            .parse(new java.io.ByteArrayInputStream(xmlContent.getBytes(StandardCharsets.UTF_8)));
        document.getDocumentElement().normalize();

        List<ImportRecord> records = new ArrayList<>();
        NodeList assessments = document.getElementsByTagName("Assessment");
        for (int i = 0; i < assessments.getLength(); i++) {
            records.add(mapElement((Element) assessments.item(i), ImportRecordType.ASSESSMENT));
        }
        NodeList events = document.getElementsByTagName("Event");
        for (int i = 0; i < events.getLength(); i++) {
            records.add(mapElement((Element) events.item(i), ImportRecordType.EVENT));
        }
        return records;
    }

    private List<ImportRecord> parseVendorFeed(String jsonContent) throws JsonProcessingException {
        JsonNode root = objectMapper.readTree(jsonContent);
        ArrayNode arrayNode = root.has("records") && root.get("records").isArray()
            ? (ArrayNode) root.get("records")
            : objectMapper.createArrayNode().add(root);

        List<ImportRecord> records = new ArrayList<>();
        for (JsonNode node : arrayNode) {
            records.add(mapJson(node));
        }
        return records;
    }

    private ImportRecord mapRow(String[] values, Map<String, Integer> columns, String source) {
        String type = read(values, columns, "recordtype");
        UUID enrollmentId = UUID.fromString(read(values, columns, "enrollmentid"));
        UUID clientId = UUID.fromString(read(values, columns, "clientid"));
        UUID consentId = UUID.fromString(read(values, columns, "consentid"));
        UUID consentLedgerId = optional(values, columns, "consentledgerid").map(UUID::fromString).orElse(null);
        boolean consentGranted = optional(values, columns, "consentgranted").map(Boolean::parseBoolean).orElse(true);

        ImportRecord.Builder builder = ImportRecord.builder()
            .recordType("ASSESSMENT".equalsIgnoreCase(type) ? ImportRecordType.ASSESSMENT : ImportRecordType.EVENT)
            .enrollmentId(ProgramEnrollmentId.of(enrollmentId))
            .clientId(clientId)
            .consentId(consentId)
            .consentLedgerId(consentLedgerId)
            .consentGranted(consentGranted)
            .hashAlgorithm(optional(values, columns, "hashalgorithm")
                .map(val -> CeHashAlgorithm.valueOf(val.toUpperCase(Locale.ROOT)))
                .orElse(CeHashAlgorithm.SHA256_SALT))
            .encryptionKeyId(read(values, columns, "encryptionkeyid"))
            .encryptionScheme(optional(values, columns, "encryptionscheme").orElse(null))
            .shareScopes(optional(values, columns, "sharescopes")
                .map(this::parseShareScopes)
                .orElseGet(() -> EnumSet.noneOf(CeShareScope.class)))
            .encryptionMetadata(parseMetadata(optional(values, columns, "encryptionmetadata").orElse(null)))
            .encryptionTags(parseTags(optional(values, columns, "encryptiontags").orElse(null)))
            .sourceSystem(source);

        if (builder.recordType == ImportRecordType.ASSESSMENT) {
            builder.assessmentDate(LocalDate.parse(read(values, columns, "assessmentdate")))
                .assessmentType(CeAssessmentType.valueOf(read(values, columns, "assessmenttype").toUpperCase(Locale.ROOT)))
                .assessmentLevel(optional(values, columns, "assessmentlevel")
                    .map(val -> CeAssessmentLevel.valueOf(val.toUpperCase(Locale.ROOT))).orElse(null))
                .score(optional(values, columns, "score").map(Double::valueOf).orElse(null))
                .toolUsed(optional(values, columns, "toolused").orElse(null))
                .prioritizationStatus(optional(values, columns, "prioritizationstatus")
                    .map(val -> CePrioritizationStatus.valueOf(val.toUpperCase(Locale.ROOT))).orElse(null))
                .location(optional(values, columns, "location").orElse(null))
                .recipientOrganization(optional(values, columns, "recipientorganization").orElse(null));
        } else {
            builder.eventDate(LocalDate.parse(read(values, columns, "eventdate")))
                .eventType(CeEventType.valueOf(read(values, columns, "eventtype").toUpperCase(Locale.ROOT)))
                .eventStatus(CeEventStatus.valueOf(read(values, columns, "eventstatus").toUpperCase(Locale.ROOT)))
                .eventResult(optional(values, columns, "eventresult")
                    .map(val -> CeEventResult.valueOf(val.toUpperCase(Locale.ROOT))).orElse(null))
                .referralDestination(optional(values, columns, "referraldestination").orElse(null))
                .outcomeDate(optional(values, columns, "outcomedate").map(LocalDate::parse).orElse(null));
        }

        return builder.build();
    }

    private ImportRecord mapElement(Element element, ImportRecordType type) {
        ImportRecord.Builder builder = ImportRecord.builder()
            .recordType(type)
            .enrollmentId(ProgramEnrollmentId.of(UUID.fromString(element.getAttribute("EnrollmentId"))))
            .clientId(UUID.fromString(element.getAttribute("ClientId")))
            .consentId(UUID.fromString(element.getAttribute("ConsentId")))
            .consentLedgerId(optionalAttribute(element, "ConsentLedgerId").map(UUID::fromString).orElse(null))
            .consentGranted(Boolean.parseBoolean(element.getAttribute("ConsentGranted")))
            .hashAlgorithm(optionalAttribute(element, "HashAlgorithm")
                .map(CeHashAlgorithm::valueOf).orElse(CeHashAlgorithm.SHA256_SALT))
            .encryptionKeyId(element.getAttribute("EncryptionKeyId"))
            .encryptionScheme(optionalAttribute(element, "EncryptionScheme").orElse(null))
            .shareScopes(parseShareScopes(optionalAttribute(element, "ShareScopes").orElse(null)))
            .encryptionMetadata(parseMetadata(optionalAttribute(element, "EncryptionMetadata").orElse(null)))
            .encryptionTags(parseTags(optionalAttribute(element, "EncryptionTags").orElse(null)))
            .sourceSystem(optionalAttribute(element, "SourceSystem").orElse("HMIS_XML"));

        if (type == ImportRecordType.ASSESSMENT) {
            builder.assessmentDate(LocalDate.parse(element.getAttribute("AssessmentDate")))
                .assessmentType(CeAssessmentType.valueOf(element.getAttribute("AssessmentType")))
                .assessmentLevel(optionalAttribute(element, "AssessmentLevel")
                    .map(CeAssessmentLevel::valueOf).orElse(null))
                .score(optionalAttribute(element, "Score").map(Double::valueOf).orElse(null))
                .toolUsed(optionalAttribute(element, "ToolUsed").orElse(null))
                .prioritizationStatus(optionalAttribute(element, "PrioritizationStatus")
                    .map(CePrioritizationStatus::valueOf).orElse(null))
                .location(optionalAttribute(element, "Location").orElse(null))
                .recipientOrganization(optionalAttribute(element, "RecipientOrganization").orElse(null));
        } else {
            builder.eventDate(LocalDate.parse(element.getAttribute("EventDate")))
                .eventType(CeEventType.valueOf(element.getAttribute("EventType")))
                .eventStatus(CeEventStatus.valueOf(element.getAttribute("EventStatus")))
                .eventResult(optionalAttribute(element, "EventResult")
                    .map(CeEventResult::valueOf).orElse(null))
                .referralDestination(optionalAttribute(element, "ReferralDestination").orElse(null))
                .outcomeDate(optionalAttribute(element, "OutcomeDate").map(LocalDate::parse).orElse(null));
        }

        return builder.build();
    }

    private ImportRecord mapJson(JsonNode node) {
        ImportRecordType type = "ASSESSMENT".equalsIgnoreCase(node.get("recordType").asText())
            ? ImportRecordType.ASSESSMENT
            : ImportRecordType.EVENT;
        ImportRecord.Builder builder = ImportRecord.builder()
            .recordType(type)
            .enrollmentId(ProgramEnrollmentId.of(UUID.fromString(node.get("enrollmentId").asText())))
            .clientId(UUID.fromString(node.get("clientId").asText()))
            .consentId(UUID.fromString(node.get("consentId").asText()))
            .consentLedgerId(node.hasNonNull("consentLedgerId") ? UUID.fromString(node.get("consentLedgerId").asText()) : null)
            .consentGranted(node.path("consentGranted").asBoolean(true))
            .hashAlgorithm(node.hasNonNull("hashAlgorithm")
                ? CeHashAlgorithm.valueOf(node.get("hashAlgorithm").asText())
                : CeHashAlgorithm.SHA256_SALT)
            .encryptionKeyId(node.get("encryptionKeyId").asText())
            .encryptionScheme(node.hasNonNull("encryptionScheme") ? node.get("encryptionScheme").asText() : null)
            .shareScopes(parseShareScopes(jsonArrayToString(node, "shareScopes")))
            .encryptionMetadata(parseJsonMetadata(node.get("encryptionMetadata")))
            .encryptionTags(parseJsonTags(node.get("encryptionTags")))
            .sourceSystem(node.path("sourceSystem").asText("VENDOR_FEED"));

        if (type == ImportRecordType.ASSESSMENT) {
            builder.assessmentDate(LocalDate.parse(node.get("assessmentDate").asText()))
                .assessmentType(CeAssessmentType.valueOf(node.get("assessmentType").asText()))
                .assessmentLevel(node.hasNonNull("assessmentLevel")
                    ? CeAssessmentLevel.valueOf(node.get("assessmentLevel").asText())
                    : null)
                .score(node.hasNonNull("score") ? node.get("score").asDouble() : null)
                .toolUsed(node.hasNonNull("toolUsed") ? node.get("toolUsed").asText() : null)
                .prioritizationStatus(node.hasNonNull("prioritizationStatus")
                    ? CePrioritizationStatus.valueOf(node.get("prioritizationStatus").asText())
                    : null)
                .location(node.hasNonNull("location") ? node.get("location").asText() : null)
                .recipientOrganization(node.hasNonNull("recipientOrganization") ? node.get("recipientOrganization").asText() : null);
        } else {
            builder.eventDate(LocalDate.parse(node.get("eventDate").asText()))
                .eventType(CeEventType.valueOf(node.get("eventType").asText()))
                .eventStatus(CeEventStatus.valueOf(node.get("status").asText()))
                .eventResult(node.hasNonNull("result") ? CeEventResult.valueOf(node.get("result").asText()) : null)
                .referralDestination(node.hasNonNull("referralDestination") ? node.get("referralDestination").asText() : null)
                .outcomeDate(node.hasNonNull("outcomeDate") ? LocalDate.parse(node.get("outcomeDate").asText()) : null);
        }

        return builder.build();
    }

    private String read(String[] values, Map<String, Integer> columns, String column) {
        return optional(values, columns, column)
            .orElseThrow(() -> new IllegalArgumentException("Missing column: " + column));
    }

    private Optional<String> optional(String[] values, Map<String, Integer> columns, String column) {
        Integer idx = columns.get(column.toLowerCase(Locale.ROOT));
        if (idx == null || idx >= values.length) return Optional.empty();
        String value = values[idx].trim();
        return value.isEmpty() ? Optional.empty() : Optional.of(value);
    }

    private Optional<String> optionalAttribute(Element element, String name) {
        String value = element.getAttribute(name);
        return value == null || value.isBlank() ? Optional.empty() : Optional.of(value);
    }

    private Set<CeShareScope> parseShareScopes(String scopes) {
        if (scopes == null || scopes.isBlank()) {
            return Collections.emptySet();
        }
        return Arrays.stream(scopes.split("[;,]"))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .map(s -> CeShareScope.valueOf(s.toUpperCase(Locale.ROOT)))
            .collect(Collectors.toCollection(() -> EnumSet.noneOf(CeShareScope.class)));
    }

    private Map<String, String> parseMetadata(String metadata) {
        if (metadata == null || metadata.isBlank()) {
            return Map.of();
        }
        Map<String, String> map = new LinkedHashMap<>();
        Arrays.stream(metadata.split("\n"))
            .map(String::trim)
            .filter(line -> !line.isEmpty())
            .forEach(line -> {
                int idx = line.indexOf('=');
                if (idx > 0) {
                    String key = line.substring(0, idx).trim();
                    String value = line.substring(idx + 1).trim();
                    if (!key.isEmpty()) {
                        map.put(key, value);
                    }
                }
            });
        return map;
    }

    private Map<String, String> parseJsonMetadata(JsonNode node) {
        if (node == null || node.isNull()) {
            return Map.of();
        }
        Map<String, String> map = new LinkedHashMap<>();
        node.fields().forEachRemaining(entry -> map.put(entry.getKey(), entry.getValue().asText()));
        return map;
    }

    private List<String> parseTags(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
            .map(String::trim)
            .filter(tag -> !tag.isEmpty())
            .collect(Collectors.toList());
    }

    private List<String> parseJsonTags(JsonNode node) {
        if (node == null || !node.isArray()) {
            return List.of();
        }
        List<String> tags = new ArrayList<>();
        node.forEach(tagNode -> tags.add(tagNode.asText()));
        return tags;
    }

    private String jsonArrayToString(JsonNode node, String field) {
        if (node == null || !node.has(field) || !node.get(field).isArray()) {
            return null;
        }
        return node.get(field).size() == 0
            ? null
            : node.get(field)
                .elements()
                .next()
                .textValue();
    }

    private String hash(ImportRecord record) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(record.consentId().toString().getBytes(StandardCharsets.UTF_8));
            digest.update(record.enrollmentId().value().toString().getBytes(StandardCharsets.UTF_8));
            record.shareScopes().stream()
                .sorted(Comparator.comparing(Enum::name))
                .forEach(scope -> digest.update(scope.name().getBytes(StandardCharsets.UTF_8)));
            return bytesToHex(digest.digest());
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }

    private String bytesToHex(byte[] bytes) {
        StringBuilder builder = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

    private ImportRecord mapJson(JsonNode node, ImportRecordType type) {
        return null;
    }

    private interface ImportSupplier {
        List<ImportRecord> get() throws Exception;
    }

    public record ImportOptions(String sourceSystem,
                                String initiatedBy,
                                char delimiter,
                                String fileName) {
        public ImportOptions {
            Objects.requireNonNull(sourceSystem, "sourceSystem");
        }

        public char delimiter() {
            return delimiter == '\0' ? ',' : delimiter;
        }
    }

    public record CeImportResult(UUID jobId,
                                 int successfulRecords,
                                 int failedRecords,
                                 int warnings) {
    }

    private enum ImportRecordType {
        ASSESSMENT,
        EVENT
    }

    private static final class ImportRecord {
        private final ImportRecordType recordType;
        private final ProgramEnrollmentId enrollmentId;
        private final UUID clientId;
        private final UUID consentId;
        private final UUID consentLedgerId;
        private final boolean consentGranted;
        private final LocalDate assessmentDate;
        private final CeAssessmentType assessmentType;
        private final CeAssessmentLevel assessmentLevel;
        private final String toolUsed;
        private final Double score;
        private final CePrioritizationStatus prioritizationStatus;
        private final String location;
        private final String recipientOrganization;
        private final LocalDate eventDate;
        private final CeEventType eventType;
        private final CeEventStatus eventStatus;
        private final CeEventResult eventResult;
        private final String referralDestination;
        private final LocalDate outcomeDate;
        private final CeHashAlgorithm hashAlgorithm;
        private final String encryptionKeyId;
        private final String encryptionScheme;
        private final Map<String, String> encryptionMetadata;
        private final List<String> encryptionTags;
        private final Set<CeShareScope> shareScopes;
        private final String sourceSystem;
        private final String warning;

        private ImportRecord(Builder builder) {
            this.recordType = builder.recordType;
            this.enrollmentId = builder.enrollmentId;
            this.clientId = builder.clientId;
            this.consentId = builder.consentId;
            this.consentLedgerId = builder.consentLedgerId;
            this.consentGranted = builder.consentGranted;
            this.assessmentDate = builder.assessmentDate;
            this.assessmentType = builder.assessmentType;
            this.assessmentLevel = builder.assessmentLevel;
            this.toolUsed = builder.toolUsed;
            this.score = builder.score;
            this.prioritizationStatus = builder.prioritizationStatus;
            this.location = builder.location;
            this.recipientOrganization = builder.recipientOrganization;
            this.eventDate = builder.eventDate;
            this.eventType = builder.eventType;
            this.eventStatus = builder.eventStatus;
            this.eventResult = builder.eventResult;
            this.referralDestination = builder.referralDestination;
            this.outcomeDate = builder.outcomeDate;
            this.hashAlgorithm = builder.hashAlgorithm;
            this.encryptionKeyId = builder.encryptionKeyId;
            this.encryptionScheme = builder.encryptionScheme;
            this.encryptionMetadata = builder.encryptionMetadata;
            this.encryptionTags = builder.encryptionTags;
            this.shareScopes = builder.shareScopes;
            this.sourceSystem = builder.sourceSystem;
            this.warning = builder.warning;
        }

        static Builder builder() {
            return new Builder();
        }

        ImportRecordType recordType() { return recordType; }
        ProgramEnrollmentId enrollmentId() { return enrollmentId; }
        UUID clientId() { return clientId; }
        UUID consentId() { return consentId; }
        Optional<UUID> consentLedgerId() { return Optional.ofNullable(consentLedgerId); }
        boolean consentGranted() { return consentGranted; }
        Optional<LocalDate> assessmentDate() { return Optional.ofNullable(assessmentDate); }
        Optional<CeAssessmentType> assessmentType() { return Optional.ofNullable(assessmentType); }
        Optional<CeAssessmentLevel> assessmentLevel() { return Optional.ofNullable(assessmentLevel); }
        Optional<String> toolUsed() { return Optional.ofNullable(toolUsed); }
        Optional<Double> score() { return Optional.ofNullable(score); }
        Optional<CePrioritizationStatus> prioritizationStatus() { return Optional.ofNullable(prioritizationStatus); }
        Optional<String> location() { return Optional.ofNullable(location); }
        Optional<String> recipientOrganization() { return Optional.ofNullable(recipientOrganization); }
        Optional<LocalDate> eventDate() { return Optional.ofNullable(eventDate); }
        Optional<CeEventType> eventType() { return Optional.ofNullable(eventType); }
        Optional<CeEventStatus> eventStatus() { return Optional.ofNullable(eventStatus); }
        Optional<CeEventResult> eventResult() { return Optional.ofNullable(eventResult); }
        Optional<String> referralDestination() { return Optional.ofNullable(referralDestination); }
        Optional<LocalDate> outcomeDate() { return Optional.ofNullable(outcomeDate); }
        CeHashAlgorithm hashAlgorithm() { return hashAlgorithm; }
        String encryptionKeyId() { return encryptionKeyId; }
        Optional<String> encryptionScheme() { return Optional.ofNullable(encryptionScheme); }
        Map<String, String> encryptionMetadata() { return encryptionMetadata; }
        List<String> encryptionTags() { return encryptionTags; }
        Set<CeShareScope> shareScopes() { return shareScopes; }
        String sourceSystem() { return sourceSystem; }
        Optional<String> warning() { return Optional.ofNullable(warning); }

        String hashSource() {
            return String.join("|", List.of(
                consentId.toString(),
                enrollmentId.value().toString(),
                recordType.name(),
                assessmentDate().map(LocalDate::toString).orElse(""),
                eventDate().map(LocalDate::toString).orElse(""),
                encryptionKeyId,
                encryptionScheme == null ? "" : encryptionScheme,
                shareScopes.stream().sorted(Comparator.comparing(Enum::name)).map(Enum::name).collect(Collectors.joining(";"))
            ));
        }

        static final class Builder {
            private ImportRecordType recordType;
            private ProgramEnrollmentId enrollmentId;
            private UUID clientId;
            private UUID consentId;
            private UUID consentLedgerId;
            private boolean consentGranted = true;
            private LocalDate assessmentDate;
            private CeAssessmentType assessmentType;
            private CeAssessmentLevel assessmentLevel;
            private String toolUsed;
            private Double score;
            private CePrioritizationStatus prioritizationStatus;
            private String location;
            private String recipientOrganization;
            private LocalDate eventDate;
            private CeEventType eventType;
            private CeEventStatus eventStatus;
            private CeEventResult eventResult;
            private String referralDestination;
            private LocalDate outcomeDate;
            private CeHashAlgorithm hashAlgorithm = CeHashAlgorithm.SHA256_SALT;
            private String encryptionKeyId;
            private String encryptionScheme;
            private Map<String, String> encryptionMetadata = Map.of();
            private List<String> encryptionTags = List.of();
            private Set<CeShareScope> shareScopes = Set.of();
            private String sourceSystem = DEFAULT_SOURCE;
            private String warning;

            Builder recordType(ImportRecordType recordType) {
                this.recordType = recordType;
                return this;
            }

            Builder enrollmentId(ProgramEnrollmentId enrollmentId) {
                this.enrollmentId = enrollmentId;
                return this;
            }

            Builder clientId(UUID clientId) {
                this.clientId = clientId;
                return this;
            }

            Builder consentId(UUID consentId) {
                this.consentId = consentId;
                return this;
            }

            Builder consentLedgerId(UUID consentLedgerId) {
                this.consentLedgerId = consentLedgerId;
                return this;
            }

            Builder consentGranted(boolean consentGranted) {
                this.consentGranted = consentGranted;
                return this;
            }

            Builder assessmentDate(LocalDate assessmentDate) {
                this.assessmentDate = assessmentDate;
                return this;
            }

            Builder assessmentType(CeAssessmentType assessmentType) {
                this.assessmentType = assessmentType;
                return this;
            }

            Builder assessmentLevel(CeAssessmentLevel assessmentLevel) {
                this.assessmentLevel = assessmentLevel;
                return this;
            }

            Builder toolUsed(String toolUsed) {
                this.toolUsed = toolUsed;
                return this;
            }

            Builder score(Double score) {
                this.score = score;
                return this;
            }

            Builder prioritizationStatus(CePrioritizationStatus prioritizationStatus) {
                this.prioritizationStatus = prioritizationStatus;
                return this;
            }

            Builder location(String location) {
                this.location = location;
                return this;
            }

            Builder recipientOrganization(String recipientOrganization) {
                this.recipientOrganization = recipientOrganization;
                return this;
            }

            Builder eventDate(LocalDate eventDate) {
                this.eventDate = eventDate;
                return this;
            }

            Builder eventType(CeEventType eventType) {
                this.eventType = eventType;
                return this;
            }

            Builder eventStatus(CeEventStatus eventStatus) {
                this.eventStatus = eventStatus;
                return this;
            }

            Builder eventResult(CeEventResult eventResult) {
                this.eventResult = eventResult;
                return this;
            }

            Builder referralDestination(String referralDestination) {
                this.referralDestination = referralDestination;
                return this;
            }

            Builder outcomeDate(LocalDate outcomeDate) {
                this.outcomeDate = outcomeDate;
                return this;
            }

            Builder hashAlgorithm(CeHashAlgorithm hashAlgorithm) {
                this.hashAlgorithm = hashAlgorithm;
                return this;
            }

            Builder encryptionKeyId(String encryptionKeyId) {
                this.encryptionKeyId = encryptionKeyId;
                return this;
            }

            Builder encryptionScheme(String encryptionScheme) {
                this.encryptionScheme = encryptionScheme;
                return this;
            }

            Builder encryptionMetadata(Map<String, String> encryptionMetadata) {
                this.encryptionMetadata = encryptionMetadata;
                return this;
            }

            Builder encryptionTags(List<String> encryptionTags) {
                this.encryptionTags = encryptionTags;
                return this;
            }

            Builder shareScopes(Set<CeShareScope> shareScopes) {
                this.shareScopes = shareScopes;
                return this;
            }

            Builder sourceSystem(String sourceSystem) {
                this.sourceSystem = sourceSystem;
                return this;
            }

            Builder warning(String warning) {
                this.warning = warning;
                return this;
            }

            ImportRecord build() {
                Objects.requireNonNull(recordType, "recordType");
                Objects.requireNonNull(enrollmentId, "enrollmentId");
                Objects.requireNonNull(clientId, "clientId");
                Objects.requireNonNull(consentId, "consentId");
                Objects.requireNonNull(encryptionKeyId, "encryptionKeyId");
                return new ImportRecord(this);
            }
        }
    }
}
