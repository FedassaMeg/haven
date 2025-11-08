package org.haven.intake;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

/**
 * Spring configuration for the Intake module
 *
 * This module provides:
 * - Pre-intake contact management (temporary client records)
 * - Progressive intake workflow (Steps 1-10)
 * - Client promotion (temp → full client)
 * - VAWA-compliant intake process
 */
@Configuration
@ComponentScan(basePackages = {
    "org.haven.intake.application",
    "org.haven.intake.domain",
    "org.haven.intake.infrastructure"
})
@EnableJpaRepositories(basePackages = "org.haven.intake.infrastructure.persistence")
@EntityScan(basePackages = "org.haven.intake.infrastructure.persistence")
public class IntakeModuleConfiguration {
    // Spring will auto-discover @Service, @Repository, @Component beans
}
