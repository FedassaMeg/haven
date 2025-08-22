package org.haven.api;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = "org.haven")
@EnableJpaRepositories(basePackages = {"org.haven.**.infrastructure.persistence", "org.haven.readmodels.infrastructure"})
@EntityScan(basePackages = {"org.haven.**.infrastructure.persistence", "org.haven.readmodels.infrastructure"})
public class ApiApplication {
	public static void main(String[] args) {
		SpringApplication.run(ApiApplication.class, args);
	}
}
