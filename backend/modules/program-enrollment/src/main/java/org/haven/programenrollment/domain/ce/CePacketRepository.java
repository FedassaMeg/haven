package org.haven.programenrollment.domain.ce;

import org.haven.clientprofile.domain.ClientId;
import org.haven.clientprofile.domain.consent.ConsentId;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CePacketRepository {

    CePacket save(CePacket packet);

    Optional<CePacket> findById(CePacketId packetId);

    Optional<CePacket> findActiveByConsent(ConsentId consentId);

    List<CePacket> findByClient(ClientId clientId);

    void revokePacketsForConsent(ConsentId consentId, UUID ledgerEntryId);
}
