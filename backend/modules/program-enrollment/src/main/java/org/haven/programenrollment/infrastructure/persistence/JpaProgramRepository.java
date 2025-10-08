package org.haven.programenrollment.infrastructure.persistence;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface JpaProgramRepository extends JpaRepository<JpaProgramEntity, UUID> {
    
    List<JpaProgramEntity> findByActiveTrue();
    
    Optional<JpaProgramEntity> findByIdAndActiveTrue(UUID id);
    
    List<JpaProgramEntity> findByHmisProjectType(Integer hmisProjectType);
    
    List<JpaProgramEntity> findByJointProjectGroupCode(String jointProjectGroupCode);
    
    @Query("SELECT p FROM JpaProgramEntity p WHERE p.hmisProjectType IN (2, 15) AND p.active = true")
    List<JpaProgramEntity> findThPrograms();
    
    @Query("SELECT p FROM JpaProgramEntity p WHERE p.hmisProjectType IN (13, 15) AND p.active = true")
    List<JpaProgramEntity> findRrhPrograms();
    
    @Query("SELECT p FROM JpaProgramEntity p WHERE p.jointProjectGroupCode = :groupCode AND p.active = true")
    List<JpaProgramEntity> findJointProjectComponents(@Param("groupCode") String groupCode);
    
    @Query("""
        SELECT p FROM JpaProgramEntity p 
        WHERE p.jointProjectGroupCode IS NOT NULL 
        AND p.hmisProjectType IN (2, 13, 15) 
        AND p.active = true
        """)
    List<JpaProgramEntity> findAllJointThRrhPrograms();
}