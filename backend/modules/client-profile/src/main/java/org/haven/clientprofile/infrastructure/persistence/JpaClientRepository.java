package org.haven.clientprofile.infrastructure.persistence;

import org.haven.clientprofile.domain.Client;
import org.haven.clientprofile.domain.ClientId;
import org.haven.clientprofile.domain.ClientRepository;
import org.haven.shared.vo.HumanName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

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
    
    @Override
    default List<Client> findByName(HumanName name) {
        return findByFirstNameAndLastName(name.getFirstName(), name.getLastName()).stream()
            .map(JpaClientEntity::toDomain)
            .collect(Collectors.toList());
    }
    
    List<JpaClientEntity> findByFirstNameAndLastName(String firstName, String lastName);
    
    @Override
    default List<Client> findByNameContaining(String nameFragment) {
        return findByFirstNameContainingOrLastNameContaining(nameFragment, nameFragment).stream()
            .map(JpaClientEntity::toDomain)
            .collect(Collectors.toList());
    }
    
    List<JpaClientEntity> findByFirstNameContainingOrLastNameContaining(String firstNameFragment, String lastNameFragment);
    
    @Override
    default List<Client> findActiveClients() {
        // For now, return all clients - in a full implementation you'd check active status
        return findAll().stream()
            .map(JpaClientEntity::toDomain)
            .collect(Collectors.toList());
    }
    
    @Override
    default Optional<Client> findByExternalId(String externalId) {
        // For now, external ID is not implemented in the entity
        // You would need to add an external_id column to the clients table
        return Optional.empty();
    }
    
    @Override
    default void delete(Client client) {
        deleteById(client.getId().value());
    }
    
    @Override
    default ClientId nextId() {
        return ClientId.generate();
    }
}
