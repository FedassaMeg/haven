package org.haven.programenrollment.infrastructure.persistence;

import org.haven.programenrollment.domain.Program;
import org.haven.programenrollment.domain.ProgramRepository;
import org.haven.shared.vo.hmis.HmisProjectType;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Repository
public class ProgramRepositoryImpl implements ProgramRepository {
    
    private final JpaProgramRepository jpaRepository;
    
    public ProgramRepositoryImpl(JpaProgramRepository jpaRepository) {
        this.jpaRepository = jpaRepository;
    }
    
    @Override
    public Optional<Program> findById(UUID id) {
        return jpaRepository.findById(id).map(this::toDomain);
    }
    
    @Override
    public List<Program> findActivePrograms() {
        return jpaRepository.findByActiveTrue().stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Program> findByJointProjectGroupCode(String groupCode) {
        return jpaRepository.findByJointProjectGroupCode(groupCode).stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Program> findThPrograms() {
        return jpaRepository.findThPrograms().stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Program> findRrhPrograms() {
        return jpaRepository.findRrhPrograms().stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }
    
    @Override
    public List<Program> findJointThRrhPrograms() {
        return jpaRepository.findAllJointThRrhPrograms().stream()
            .map(this::toDomain)
            .collect(Collectors.toList());
    }
    
    @Override
    public void save(Program program) {
        JpaProgramEntity entity = toEntity(program);
        jpaRepository.save(entity);
    }
    
    private Program toDomain(JpaProgramEntity entity) {
        HmisProjectType projectType = entity.getHmisProjectType() != null 
            ? HmisProjectType.fromHmisTypeId(entity.getHmisProjectType()) 
            : null;
            
        return new Program(
            entity.getId(),
            entity.getName(),
            entity.getDescription(),
            projectType,
            entity.getJointProjectGroupCode(),
            entity.getActive(),
            entity.getCreatedAt()
        );
    }
    
    private JpaProgramEntity toEntity(Program program) {
        Integer hmisProjectTypeId = program.getHmisProjectType() != null 
            ? program.getHmisProjectType().getHmisTypeId() 
            : null;
            
        return new JpaProgramEntity(
            program.getId(),
            program.getName(),
            program.getDescription(),
            hmisProjectTypeId,
            program.getJointProjectGroupCode(),
            program.isActive(),
            program.getCreatedAt()
        );
    }
}