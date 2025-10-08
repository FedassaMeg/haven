package org.haven.eventstore.infrastructure;

import jakarta.persistence.*;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "event_store", schema = "haven", indexes = {
    @Index(name = "idx_event_store_aggregate_id", columnList = "aggregate_id"),
    @Index(name = "idx_event_store_event_type", columnList = "event_type"),
    @Index(name = "idx_event_store_recorded_at", columnList = "recorded_at")
})
public class JpaEventStoreEntity {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "aggregate_id", nullable = false)
    private UUID aggregateId;
    
    @Column(name = "sequence", nullable = false)
    private Long sequence;
    
    @Column(name = "event_type", nullable = false, length = 200)
    private String eventType;
    
    @Column(name = "event_data", nullable = false, columnDefinition = "TEXT")
    private String eventData;
    
    @Column(name = "recorded_at", nullable = false)
    private Instant recordedAt;
    
    @Version
    private Long version;
    
    protected JpaEventStoreEntity() {
        // For JPA
    }
    
    public JpaEventStoreEntity(UUID aggregateId, Long sequence, String eventType, String eventData, Instant recordedAt) {
        this.aggregateId = aggregateId;
        this.sequence = sequence;
        this.eventType = eventType;
        this.eventData = eventData;
        this.recordedAt = recordedAt;
    }
    
    public Long getId() { return id; }
    public UUID getAggregateId() { return aggregateId; }
    public Long getSequence() { return sequence; }
    public String getEventType() { return eventType; }
    public String getEventData() { return eventData; }
    public Instant getRecordedAt() { return recordedAt; }
    public Long getVersion() { return version; }
}