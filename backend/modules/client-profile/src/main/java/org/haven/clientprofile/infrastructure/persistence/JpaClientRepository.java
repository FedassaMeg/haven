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
public interface JpaClientRepository extends JpaRepository<JpaClientEntity, UUID> {

    /**
     * Find clients by exact first and last name match
     */
    List<JpaClientEntity> findByFirstNameAndLastName(String firstName, String lastName);

    /**
     * Find clients by partial name match (searches both first and last names)
     */
    List<JpaClientEntity> findByFirstNameContainingOrLastNameContaining(String firstNameFragment, String lastNameFragment);
}
