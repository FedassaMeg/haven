package org.haven.clientprofile.infrastructure.persistence;

import org.haven.clientprofile.domain.Client;
import org.haven.clientprofile.domain.ClientId;
import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "clients", schema = "haven")
public class JpaClientEntity {
    
    @Id
    private UUID id;
    
    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;
    
    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;
    
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;
    
    @Version
    private Long version;
    
    // Constructors
    protected JpaClientEntity() {
        // JPA requires default constructor
    }
    
    public JpaClientEntity(UUID id, String firstName, String lastName, Instant createdAt) {
        this.id = id;
        this.firstName = firstName;
        this.lastName = lastName;
        this.createdAt = createdAt;
    }
    
    // Factory methods
    public static JpaClientEntity fromDomain(Client client) {
        return new JpaClientEntity(
            client.id().value(),
            client.getFirstName(),
            client.getLastName(),
            client.getCreatedAt()
        );
    }
    
    public Client toDomain() {
        // For now, return a simple reconstruction
        // In a full implementation, you'd replay events from the event store
        Client client = Client.create(firstName, lastName);
        return client;
    }
    
    // Getters and setters
    public UUID getId() { return id; }
    public void setId(UUID id) { this.id = id; }
    
    public String getFirstName() { return firstName; }
    public void setFirstName(String firstName) { this.firstName = firstName; }
    
    public String getLastName() { return lastName; }
    public void setLastName(String lastName) { this.lastName = lastName; }
    
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    
    public Long getVersion() { return version; }
    public void setVersion(Long version) { this.version = version; }
}
