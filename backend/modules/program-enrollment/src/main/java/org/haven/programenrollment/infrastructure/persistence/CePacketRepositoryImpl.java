package org.haven.programenrollment.infrastructure.persistence;

import org.haven.clientprofile.domain.ClientId;
import org.haven.clientprofile.domain.consent.ConsentId;
import org.haven.clientprofile.domain.consent.ConsentStatus;
import org.haven.programenrollment.domain.ce.CePacket;
import org.haven.programenrollment.domain.ce.CePacketId;
import org.haven.programenrollment.domain.ce.CePacketRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public class CePacketRepositoryImpl implements CePacketRepository {

    private final JpaCePacketSpringRepository repository;

    public CePacketRepositoryImpl(JpaCePacketSpringRepository repository) {
        this.repository = repository;
    }

    @Override
    public CePacket save(CePacket packet) {
        JpaCePacketEntity entity = new JpaCePacketEntity(packet);
        JpaCePacketEntity persisted = repository.save(entity);
        return persisted.toDomain();
    }

    @Override
    public Optional<CePacket> findById(CePacketId packetId) {
        return repository.findById(packetId.value()).map(JpaCePacketEntity::toDomain);
    }

    @Override
    public Optional<CePacket> findActiveByConsent(ConsentId consentId) {
        return repository.findFirstByConsentIdAndConsentStatusOrderByUpdatedAtDesc(consentId.value(), ConsentStatus.GRANTED)
            .map(JpaCePacketEntity::toDomain)
            .filter(CePacket::isConsentActive);
    }

    @Override
    public List<CePacket> findByClient(ClientId clientId) {
        return repository.findByClientId(clientId.value()).stream()
            .map(JpaCePacketEntity::toDomain)
            .toList();
    }

    @Override
    public void revokePacketsForConsent(ConsentId consentId, UUID ledgerEntryId) {
        var now = Instant.now();
        repository.findByConsentId(consentId.value()).forEach(entity -> {
            CePacket updated = entity.toDomain()
                .toBuilder()
                .consentStatus(ConsentStatus.REVOKED)
                .ledgerEntryId(ledgerEntryId)
                .updatedAt(now)
                .build();
            repository.save(new JpaCePacketEntity(updated));
        });
    }
}
