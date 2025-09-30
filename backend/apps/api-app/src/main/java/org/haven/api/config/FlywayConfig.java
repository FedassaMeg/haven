package org.haven.api.config;

import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationStrategy;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FlywayConfig {

    @Bean
    public FlywayMigrationStrategy repairAndMigrateStrategy() {
        return flyway -> {
            Flyway.configure()
                .configuration(flyway.getConfiguration())
                .load()
                .repair();
            flyway.migrate();
        };
    }
}
