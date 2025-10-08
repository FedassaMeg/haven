package org.haven.reporting.infrastructure.config;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Spring configuration for reporting module
 * Enables transaction management and async processing
 * Note: JPA repositories are registered by ApiApplication's @EnableJpaRepositories
 */
@Configuration
@EnableTransactionManagement
@ComponentScan(basePackages = {
    "org.haven.reporting.domain",
    "org.haven.reporting.application",
    "org.haven.reporting.infrastructure",
    "org.haven.reporting.presentation"
})
@Import({
    ReportingAsyncConfiguration.class
})
public class ReportingModuleConfiguration {
    // Configuration class - beans are auto-discovered via @ComponentScan
}
