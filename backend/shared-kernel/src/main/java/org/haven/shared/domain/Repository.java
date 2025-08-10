package org.haven.shared.domain;

import org.haven.shared.Identifier;

import java.util.Optional;

/**
 * Base repository interface for DDD aggregates
 */
public interface Repository<T extends AggregateRoot<ID>, ID extends Identifier> {
    
    Optional<T> findById(ID id);
    
    void save(T aggregate);
    
    void delete(T aggregate);
    
    ID nextId();
}