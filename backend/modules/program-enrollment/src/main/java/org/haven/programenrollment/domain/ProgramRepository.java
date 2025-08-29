package org.haven.programenrollment.domain;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository interface for Program domain entities
 */
public interface ProgramRepository {
    
    Optional<Program> findById(UUID id);
    
    List<Program> findActivePrograms();
    
    List<Program> findByJointProjectGroupCode(String groupCode);
    
    List<Program> findThPrograms();
    
    List<Program> findRrhPrograms();
    
    List<Program> findJointThRrhPrograms();
    
    void save(Program program);
}