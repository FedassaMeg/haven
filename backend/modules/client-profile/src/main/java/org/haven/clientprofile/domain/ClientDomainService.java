package org.haven.clientprofile.domain;

import org.haven.shared.vo.HumanName;
import java.util.List;

/**
 * Domain service for complex client business logic
 */
public class ClientDomainService {
    
    private final ClientRepository clientRepository;
    
    public ClientDomainService(ClientRepository clientRepository) {
        this.clientRepository = clientRepository;
    }
    
    /**
     * Check for potential duplicate clients based on name similarity
     */
    public List<Client> findPotentialDuplicates(HumanName name) {
        // Simple implementation - in real world would use more sophisticated matching
        List<Client> exactMatches = clientRepository.findByName(name);
        
        if (!exactMatches.isEmpty()) {
            return exactMatches;
        }
        
        // Check for partial name matches
        String fullName = name.getFullName();
        return clientRepository.findByNameContaining(fullName);
    }
    
    /**
     * Validate client business rules before creation
     */
    public void validateClientCreation(HumanName name, Client.AdministrativeGender gender) {
        if (name == null || name.given().isEmpty() || name.family() == null) {
            throw new IllegalArgumentException("Client must have at least one given name and family name");
        }
        
        List<Client> duplicates = findPotentialDuplicates(name);
        if (!duplicates.isEmpty()) {
            // In real world, might flag for manual review rather than throw exception
            throw new ClientDuplicationException("Potential duplicate clients found with similar names");
        }
    }
    
    /**
     * Business logic for household member relationships
     */
    public boolean canAddHouseholdMember(Client client, HouseholdMember member) {
        if (!client.isActive()) {
            return false;
        }
        
        // Check for existing relationship
        return client.getHouseholdMembers().stream()
                .noneMatch(existing -> existing.getId().equals(member.getId()));
    }
    
    public static class ClientDuplicationException extends RuntimeException {
        public ClientDuplicationException(String message) {
            super(message);
        }
    }
}