package org.haven.clientprofile.domain;

import org.haven.shared.domain.Repository;
import org.haven.shared.vo.HumanName;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface ClientRepository extends Repository<Client, ClientId> {
    List<Client> findByName(HumanName name);
    List<Client> findByNameContaining(String nameFragment);
    List<Client> findActiveClients();
    Optional<Client> findByExternalId(String externalId);
    
    // Additional methods for reporting
    List<Client> findByCreatedAtBetween(Instant startDate, Instant endDate);
}
