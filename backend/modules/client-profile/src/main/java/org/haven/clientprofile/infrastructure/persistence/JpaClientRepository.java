package org.haven.clientprofile.infrastructure.persistence;

import org.haven.clientprofile.domain.Client;
import org.haven.clientprofile.domain.ClientId;
import org.haven.clientprofile.domain.ClientRepository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaClientRepository extends JpaRepository<JpaClientEntity, UUID>, ClientRepository {
    
    @Override
    default void save(Client client) {
        JpaClientEntity entity = JpaClientEntity.fromDomain(client);
        save(entity);
    }
    
    @Override
    default Optional<Client> findById(ClientId id) {
        return findById(id.value())
            .map(JpaClientEntity::toDomain);
    }
}
