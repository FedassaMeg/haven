package org.haven.programenrollment.infrastructure.config;

import org.haven.programenrollment.domain.ProgramEnrollmentRepository;
import org.haven.programenrollment.infrastructure.persistence.JpaProgramEnrollmentRepository;
import org.haven.programenrollment.infrastructure.persistence.JpaProgramEnrollmentRepositoryImpl;
import org.haven.programenrollment.infrastructure.persistence.ProgramEnrollmentAssembler;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

@Configuration
public class ProgramEnrollmentConfig {
    
    /**
     * Use JPA-backed repository implementation by default
     */
    @Bean
    @Primary
    @ConditionalOnProperty(name = "haven.enrollment.repository.type", havingValue = "jpa", matchIfMissing = true)
    public ProgramEnrollmentRepository jpaProgramEnrollmentRepository(
            JpaProgramEnrollmentRepository jpaRepository,
            ProgramEnrollmentAssembler assembler) {
        return new JpaProgramEnrollmentRepositoryImpl(jpaRepository, assembler);
    }
}