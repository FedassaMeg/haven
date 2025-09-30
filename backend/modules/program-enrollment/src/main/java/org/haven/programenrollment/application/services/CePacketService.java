package org.haven.programenrollment.application.services;

import org.haven.clientprofile.domain.ClientId;
import org.haven.clientprofile.domain.consent.Consent;
import org.haven.clientprofile.domain.consent.ConsentId;
import org.haven.programenrollment.application.security.CePacketCryptoService;
import org.haven.programenrollment.domain.ProgramEnrollmentId;
import org.haven.programenrollment.domain.ce.CeHashAlgorithm;
import org.haven.programenrollment.domain.ce.CePacket;
import org.haven.programenrollment.domain.ce.CePacketRepository;
import org.haven.programenrollment.domain.ce.CeShareScope;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class CePacketService {

    private final CePacketCryptoService cryptoService;
    private final CePacketRepository packetRepository;

    public CePacketService(CePacketCryptoService cryptoService, CePacketRepository packetRepository) {
        this.cryptoService = cryptoService;
        this.packetRepository = packetRepository;
    }

    @Transactional
    public CePacket createOrRefreshPacket(ProgramEnrollmentId enrollmentId,
                                          ClientId clientId,
                                          Consent consent,
                                          Set<CeShareScope> scopes,
                                          CeHashAlgorithm algorithm,
                                          String encryptionScheme,
                                          String encryptionKeyId,
                                          Map<String, String> encryptionMetadata,
                                          List<String> encryptionTags,
                                          UUID ledgerEntryId) {
        CePacket packet = cryptoService.createPacket(clientId, enrollmentId, consent, scopes,
            algorithm, encryptionScheme, encryptionKeyId, encryptionMetadata, encryptionTags);
        if (ledgerEntryId != null) {
            packet = packet.toBuilder()
                .ledgerEntryId(ledgerEntryId)
                .build();
        }
        return packetRepository.save(packet);
    }

    @Transactional(readOnly = true)
    public Optional<CePacket> findActivePacket(ConsentId consentId) {
        return packetRepository.findActiveByConsent(consentId);
    }

    @Transactional(readOnly = true)
    public Optional<CePacket> findById(UUID packetId) {
        return packetRepository.findById(org.haven.programenrollment.domain.ce.CePacketId.of(packetId));
    }

    @Transactional(readOnly = true)
    public java.util.List<CePacket> findByClient(ClientId clientId) {
        return packetRepository.findByClient(clientId);
    }

    @Transactional
    public void revokePackets(ConsentId consentId, UUID ledgerEntryId) {
        packetRepository.revokePacketsForConsent(consentId, ledgerEntryId);
    }
}
