package org.haven.programenrollment.application.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.haven.clientprofile.domain.ClientId;
import org.haven.programenrollment.application.security.CePacketCryptoService;
import org.haven.programenrollment.domain.ProgramEnrollmentId;
import org.haven.programenrollment.domain.ce.*;
import org.haven.programenrollment.infrastructure.persistence.JpaCeExportReceiptEntity;
import org.haven.programenrollment.infrastructure.persistence.JpaCeExportReceiptRepository;
import org.haven.shared.audit.AuditService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;
import java.io.ByteArrayOutputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Transactional
public class CeExportService {

    private static final Logger logger = LoggerFactory.getLogger(CeExportService.class);
    private static final DateTimeFormatter FILE_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss");
    private static final int GCM_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;

    private final CeAssessmentService assessmentService;
    private final CeEventService eventService;
    private final CeReferralService referralService;
    private final CePacketRepository packetRepository;
    private final CePacketCryptoService cryptoService;
    private final JpaCeExportReceiptRepository receiptRepository;
    private final ConsentLedgerUpdatePublisher ledgerUpdatePublisher;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;

    public CeExportService(
            CeAssessmentService assessmentService,
            CeEventService eventService,
            CeReferralService referralService,
            CePacketRepository packetRepository,
            CePacketCryptoService cryptoService,
            JpaCeExportReceiptRepository receiptRepository,
            ConsentLedgerUpdatePublisher ledgerUpdatePublisher,
            AuditService auditService,
            ObjectMapper objectMapper) {
        this.assessmentService = assessmentService;
        this.eventService = eventService;
        this.referralService = referralService;
        this.packetRepository = packetRepository;
        this.cryptoService = cryptoService;
        this.receiptRepository = receiptRepository;
        this.ledgerUpdatePublisher = ledgerUpdatePublisher;
        this.auditService = auditService;
        this.objectMapper = objectMapper;
    }

    /**
     * Export CE data in HUD CSV format with encryption
     */
    public CeExportResult exportHudCsv(ExportRequest request) {
        logger.info("Starting HUD CSV export for CoC: {}", request.cocId());

        List<ExportRecord> records = gatherRecords(request);
        validateConsentScopes(records, request.requiredScopes());

        StringBuilder csv = new StringBuilder();
        csv.append(buildCsvHeader()).append("\n");

        for (ExportRecord record : records) {
            csv.append(buildCsvRow(record)).append("\n");
        }

        byte[] encryptedData = encrypt(csv.toString().getBytes(StandardCharsets.UTF_8), request);
        String fileName = generateFileName(request.cocId(), "CSV", request.exportType());

        UUID receiptId = saveExportReceipt(request, fileName, records.size(), encryptedData.length);
        writeToLedger(request, receiptId, records);

        return new CeExportResult(
            receiptId,
            fileName,
            encryptedData,
            records.size(),
            request.exportType(),
            "HUD_CSV"
        );
    }

    /**
     * Export CE data in HUD XML format with encryption
     */
    public CeExportResult exportHudXml(ExportRequest request) {
        logger.info("Starting HUD XML export for CoC: {}", request.cocId());

        List<ExportRecord> records = gatherRecords(request);
        validateConsentScopes(records, request.requiredScopes());

        try {
            Document doc = buildXmlDocument(records, request);
            String xmlContent = transformToString(doc);
            byte[] encryptedData = encrypt(xmlContent.getBytes(StandardCharsets.UTF_8), request);
            String fileName = generateFileName(request.cocId(), "XML", request.exportType());

            UUID receiptId = saveExportReceipt(request, fileName, records.size(), encryptedData.length);
            writeToLedger(request, receiptId, records);

            return new CeExportResult(
                receiptId,
                fileName,
                encryptedData,
                records.size(),
                request.exportType(),
                "HUD_XML"
            );
        } catch (Exception e) {
            logger.error("Failed to generate XML export", e);
            throw new RuntimeException("Failed to generate XML export", e);
        }
    }

    /**
     * Export CE data in JSON format for vendor systems
     */
    public CeExportResult exportVendorJson(ExportRequest request) {
        logger.info("Starting vendor JSON export for CoC: {}", request.cocId());

        List<ExportRecord> records = gatherRecords(request);
        validateConsentScopes(records, request.requiredScopes());

        ObjectNode root = objectMapper.createObjectNode();
        root.put("exportId", UUID.randomUUID().toString());
        root.put("cocId", request.cocId());
        root.put("exportDate", Instant.now().toString());
        root.put("recordCount", records.size());
        root.put("exportType", request.exportType().toString());

        ArrayNode recordsArray = root.putArray("records");
        for (ExportRecord record : records) {
            recordsArray.add(buildJsonRecord(record));
        }

        try {
            String jsonContent = objectMapper.writerWithDefaultPrettyPrinter().writeValueAsString(root);
            byte[] encryptedData = encrypt(jsonContent.getBytes(StandardCharsets.UTF_8), request);
            String fileName = generateFileName(request.cocId(), "JSON", request.exportType());

            UUID receiptId = saveExportReceipt(request, fileName, records.size(), encryptedData.length);
            writeToLedger(request, receiptId, records);

            return new CeExportResult(
                receiptId,
                fileName,
                encryptedData,
                records.size(),
                request.exportType(),
                "VENDOR_JSON"
            );
        } catch (Exception e) {
            logger.error("Failed to generate JSON export", e);
            throw new RuntimeException("Failed to generate JSON export", e);
        }
    }

    private List<ExportRecord> gatherRecords(ExportRequest request) {
        List<ExportRecord> records = new ArrayList<>();

        // Gather assessments
        if (request.includeAssessments()) {
            List<CeAssessment> assessments = request.enrollmentIds().stream()
                .flatMap(enrollmentId -> assessmentService.getAssessmentsForEnrollment(enrollmentId).stream())
                .filter(a -> isInDateRange(a.getAssessmentDate(), request.startDate(), request.endDate()))
                .collect(Collectors.toList());

            for (CeAssessment assessment : assessments) {
                CePacket packet = packetRepository.findById(assessment.getPacketId())
                    .orElseThrow(() -> new IllegalStateException("Packet not found for assessment"));
                records.add(ExportRecord.fromAssessment(assessment, packet));
            }
        }

        // Gather events
        if (request.includeEvents()) {
            List<CeEvent> events = request.enrollmentIds().stream()
                .flatMap(enrollmentId -> eventService.getEventsForEnrollment(enrollmentId).stream())
                .filter(e -> isInDateRange(e.getEventDate(), request.startDate(), request.endDate()))
                .collect(Collectors.toList());

            for (CeEvent event : events) {
                CePacket packet = packetRepository.findById(event.getPacketId())
                    .orElseThrow(() -> new IllegalStateException("Packet not found for event"));
                records.add(ExportRecord.fromEvent(event, packet));
            }
        }

        // Gather referrals
        if (request.includeReferrals()) {
            List<CeReferral> referrals = request.enrollmentIds().stream()
                .flatMap(enrollmentId -> referralService.getReferralsForEnrollment(
                    enrollmentId, true, null).stream())
                .filter(r -> isInDateRange(r.getReferralDate().toLocalDate(), request.startDate(), request.endDate()))
                .collect(Collectors.toList());

            for (CeReferral referral : referrals) {
                CePacket packet = packetRepository.findById(referral.getPacketId())
                    .orElseThrow(() -> new IllegalStateException("Packet not found for referral"));
                records.add(ExportRecord.fromReferral(referral, packet));
            }
        }

        return records;
    }

    private void validateConsentScopes(List<ExportRecord> records, Set<CeShareScope> requiredScopes) {
        for (ExportRecord record : records) {
            for (CeShareScope scope : requiredScopes) {
                if (!record.packet.allowsScope(scope)) {
                    throw new IllegalStateException(
                        String.format("Record %s does not allow required scope %s", record.recordId, scope)
                    );
                }
            }
        }
    }

    private String buildCsvHeader() {
        return "RecordType,RecordId,EnrollmentId,ClientHash,Date,Type,Status,Result,Score," +
               "PrioritizationStatus,ConsentVersion,ShareScopes,HashAlgorithm,EncryptionKeyId";
    }

    private String buildCsvRow(ExportRecord record) {
        return String.join(",",
            record.recordType,
            record.recordId.toString(),
            record.enrollmentId.toString(),
            record.packet.getClientHash(),
            record.date.toString(),
            record.type != null ? record.type : "",
            record.status != null ? record.status : "",
            record.result != null ? record.result : "",
            record.score != null ? record.score.toString() : "",
            record.prioritizationStatus != null ? record.prioritizationStatus : "",
            String.valueOf(record.packet.getConsentVersion()),
            record.packet.getAllowedShareScopes().stream().map(Enum::name).collect(Collectors.joining(";")),
            record.packet.getHashAlgorithm().name(),
            record.packet.getEncryptionKeyId()
        );
    }

    private Document buildXmlDocument(List<ExportRecord> records, ExportRequest request)
            throws ParserConfigurationException {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        Document doc = factory.newDocumentBuilder().newDocument();

        Element root = doc.createElement("CEExport");
        root.setAttribute("xmlns", Hud2024CeDataDictionary.CeExportFormats.XML_NAMESPACE);
        root.setAttribute("version", Hud2024CeDataDictionary.CeExportFormats.HUD_XML_VERSION);
        root.setAttribute("exportDate", Instant.now().toString());
        root.setAttribute("cocId", request.cocId());
        doc.appendChild(root);

        for (ExportRecord record : records) {
            Element recordElement = doc.createElement(record.recordType);
            recordElement.setAttribute("RecordId", record.recordId.toString());
            recordElement.setAttribute("EnrollmentId", record.enrollmentId.toString());
            recordElement.setAttribute("ClientHash", record.packet.getClientHash());
            recordElement.setAttribute("Date", record.date.toString());
            if (record.type != null) recordElement.setAttribute("Type", record.type);
            if (record.status != null) recordElement.setAttribute("Status", record.status);
            if (record.result != null) recordElement.setAttribute("Result", record.result);
            if (record.score != null) recordElement.setAttribute("Score", record.score.toString());
            recordElement.setAttribute("ConsentVersion", String.valueOf(record.packet.getConsentVersion()));
            recordElement.setAttribute("HashAlgorithm", record.packet.getHashAlgorithm().name());
            recordElement.setAttribute("EncryptionKeyId", record.packet.getEncryptionKeyId());
            root.appendChild(recordElement);
        }

        return doc;
    }

    private ObjectNode buildJsonRecord(ExportRecord record) {
        ObjectNode node = objectMapper.createObjectNode();
        node.put("recordType", record.recordType);
        node.put("recordId", record.recordId.toString());
        node.put("enrollmentId", record.enrollmentId.toString());
        node.put("clientHash", record.packet.getClientHash());
        node.put("date", record.date.toString());
        if (record.type != null) node.put("type", record.type);
        if (record.status != null) node.put("status", record.status);
        if (record.result != null) node.put("result", record.result);
        if (record.score != null) node.put("score", record.score);
        if (record.prioritizationStatus != null) node.put("prioritizationStatus", record.prioritizationStatus);
        node.put("consentVersion", record.packet.getConsentVersion());
        node.put("hashAlgorithm", record.packet.getHashAlgorithm().name());
        node.put("encryptionKeyId", record.packet.getEncryptionKeyId());

        ArrayNode scopesArray = node.putArray("shareScopes");
        for (CeShareScope scope : record.packet.getAllowedShareScopes()) {
            scopesArray.add(scope.name());
        }

        ObjectNode encryptionMetadata = node.putObject("encryptionMetadata");
        for (Map.Entry<String, String> entry : record.packet.getEncryptionMetadata().entrySet()) {
            encryptionMetadata.put(entry.getKey(), entry.getValue());
        }

        return node;
    }

    private String transformToString(Document doc) throws Exception {
        TransformerFactory factory = TransformerFactory.newInstance();
        Transformer transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");

        StringWriter writer = new StringWriter();
        transformer.transform(new DOMSource(doc), new StreamResult(writer));
        return writer.toString();
    }

    private byte[] encrypt(byte[] data, ExportRequest request) {
        try {
            // Generate IV
            byte[] iv = new byte[GCM_IV_LENGTH];
            new SecureRandom().nextBytes(iv);

            // Get encryption key
            byte[] keyBytes = cryptoService.getEncryptionKey(request.encryptionKeyId());
            SecretKeySpec keySpec = new SecretKeySpec(keyBytes, "AES");

            // Encrypt data
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            GCMParameterSpec gcmSpec = new GCMParameterSpec(GCM_TAG_LENGTH, iv);
            cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec);

            byte[] encryptedData = cipher.doFinal(data);

            // Combine IV and encrypted data
            ByteArrayOutputStream output = new ByteArrayOutputStream();
            output.write(iv);
            output.write(encryptedData);

            return output.toByteArray();
        } catch (Exception e) {
            logger.error("Encryption failed", e);
            throw new RuntimeException("Failed to encrypt export data", e);
        }
    }

    private UUID saveExportReceipt(ExportRequest request, String fileName, int recordCount, int fileSize) {
        JpaCeExportReceiptEntity receipt = JpaCeExportReceiptEntity.createSimpleReceipt(
            UUID.randomUUID(),
            request.cocId(),
            request.exportType().toString(),
            fileName,
            recordCount,
            fileSize,
            request.encryptionKeyId(),
            request.initiatedBy()
        );

        receipt = receiptRepository.save(receipt);

        auditService.logAction("CE_DATA_EXPORTED", Map.of(
            "receiptId", receipt.getReceiptId(),
            "cocId", request.cocId(),
            "recordCount", recordCount,
            "exportType", request.exportType(),
            "initiatedBy", request.initiatedBy()
        ));

        return receipt.getReceiptId();
    }

    private void writeToLedger(ExportRequest request, UUID receiptId, List<ExportRecord> records) {
        Map<String, Object> ledgerData = Map.of(
            "eventType", "CE_EXPORT",
            "receiptId", receiptId,
            "cocId", request.cocId(),
            "exportType", request.exportType(),
            "recordCount", records.size(),
            "recordIds", records.stream().map(r -> r.recordId).collect(Collectors.toList()),
            "timestamp", Instant.now()
        );

        ledgerUpdatePublisher.publishUpdate(ledgerData);
    }

    private String generateFileName(String cocId, String format, ExportType type) {
        String timestamp = LocalDateTime.now(ZoneId.systemDefault()).format(FILE_DATE_FORMAT);
        return String.format("CE_Export_%s_%s_%s.%s.enc",
            cocId, timestamp, type, format.toLowerCase());
    }

    private boolean isInDateRange(Object date, LocalDateTime start, LocalDateTime end) {
        LocalDateTime dateTime;
        if (date instanceof java.time.LocalDate) {
            dateTime = ((java.time.LocalDate) date).atStartOfDay();
        } else if (date instanceof LocalDateTime) {
            dateTime = (LocalDateTime) date;
        } else {
            return false;
        }

        return (start == null || !dateTime.isBefore(start)) &&
               (end == null || !dateTime.isAfter(end));
    }

    public enum ExportType {
        ASSESSMENT_ONLY,
        EVENT_ONLY,
        REFERRAL_ONLY,
        ALL_RECORDS
    }

    public record ExportRequest(
        String cocId,
        List<UUID> enrollmentIds,
        LocalDateTime startDate,
        LocalDateTime endDate,
        ExportType exportType,
        Set<CeShareScope> requiredScopes,
        String encryptionKeyId,
        String initiatedBy
    ) {
        public boolean includeAssessments() {
            return exportType == ExportType.ASSESSMENT_ONLY || exportType == ExportType.ALL_RECORDS;
        }

        public boolean includeEvents() {
            return exportType == ExportType.EVENT_ONLY || exportType == ExportType.ALL_RECORDS;
        }

        public boolean includeReferrals() {
            return exportType == ExportType.REFERRAL_ONLY || exportType == ExportType.ALL_RECORDS;
        }
    }

    public record CeExportResult(
        UUID receiptId,
        String fileName,
        byte[] encryptedData,
        int recordCount,
        ExportType exportType,
        String format
    ) {}

    private static class ExportRecord {
        final String recordType;
        final UUID recordId;
        final UUID enrollmentId;
        final Object date;
        final String type;
        final String status;
        final String result;
        final Double score;
        final String prioritizationStatus;
        final CePacket packet;

        ExportRecord(String recordType, UUID recordId, UUID enrollmentId, Object date,
                    String type, String status, String result, Double score,
                    String prioritizationStatus, CePacket packet) {
            this.recordType = recordType;
            this.recordId = recordId;
            this.enrollmentId = enrollmentId;
            this.date = date;
            this.type = type;
            this.status = status;
            this.result = result;
            this.score = score;
            this.prioritizationStatus = prioritizationStatus;
            this.packet = packet;
        }

        static ExportRecord fromAssessment(CeAssessment assessment, CePacket packet) {
            return new ExportRecord(
                "Assessment",
                assessment.getRecordId(),
                assessment.getEnrollmentId().value(),
                assessment.getAssessmentDate(),
                assessment.getAssessmentType().name(),
                assessment.getAssessmentLevel() != null ? assessment.getAssessmentLevel().name() : null,
                null,
                assessment.getScore() != null ? assessment.getScore().doubleValue() : null,
                assessment.getPrioritizationStatus() != null ? assessment.getPrioritizationStatus().name() : null,
                packet
            );
        }

        static ExportRecord fromEvent(CeEvent event, CePacket packet) {
            return new ExportRecord(
                "Event",
                event.getRecordId(),
                event.getEnrollmentId().value(),
                event.getEventDate(),
                event.getEventType().name(),
                event.getStatus().name(),
                event.getResult() != null ? event.getResult().name() : null,
                null,
                null,
                packet
            );
        }

        static ExportRecord fromReferral(CeReferral referral, CePacket packet) {
            return new ExportRecord(
                "Referral",
                referral.getReferralId(),
                referral.getEnrollmentId().value(),
                referral.getReferralDate(),
                referral.getReferralType().name(),
                referral.getStatus().name(),
                referral.getResult() != null ? referral.getResult().name() : null,
                referral.getVulnerabilityScore(),
                null,
                packet
            );
        }
    }
}
