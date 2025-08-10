package org.haven.clientprofile.domain;

import org.haven.shared.domain.Repository;
import org.haven.shared.vo.HumanName;
import java.util.List;
import java.util.Optional;

public interface ClientRepository extends Repository<Client, ClientId> {
    List<Client> findByName(HumanName name);
    List<Client> findByNameContaining(String nameFragment);
    List<Client> findActiveClients();
    Optional<Client> findByExternalId(String externalId);
}
