package org.haven.programenrollment.infrastructure.persistence;

import org.haven.clientprofile.domain.consent.ConsentStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaCePacketSpringRepository extends JpaRepository<JpaCePacketEntity, UUID> {

    Optional<JpaCePacketEntity> findFirstByConsentIdAndConsentStatusOrderByUpdatedAtDesc(UUID consentId, ConsentStatus status);

    List<JpaCePacketEntity> findByClientId(UUID clientId);

    List<JpaCePacketEntity> findByConsentId(UUID consentId);
}
