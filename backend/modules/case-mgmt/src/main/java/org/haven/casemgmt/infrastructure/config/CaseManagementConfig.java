package org.haven.casemgmt.infrastructure.config;

import org.haven.casemgmt.domain.CaseDomainService;
import org.haven.casemgmt.domain.CaseRepository;
import org.haven.clientprofile.domain.ClientRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Infrastructure configuration for case management module.
 * Wires domain services and other infrastructure concerns.
 */
@Configuration
@ComponentScan(basePackages = "org.haven.casemgmt")
public class CaseManagementConfig {

    /**
     * Wire CaseDomainService with its dependencies.
     * This keeps the domain layer free of framework annotations.
     */
    @Bean
    public CaseDomainService caseDomainService(
            CaseRepository caseRepository,
            ClientRepository clientRepository) {
        return new CaseDomainService(caseRepository, clientRepository);
    }
}
