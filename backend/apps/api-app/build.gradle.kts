plugins {
  id("org.springframework.boot")
  id("io.spring.dependency-management")
  id("org.flywaydb.flyway") version "11.7.2"
  java
}

repositories {
    mavenCentral()
}

// Configure Spring Boot plugin
springBoot {
    mainClass.set("org.haven.api.ApiApplication")
}

dependencies {
  // Domain modules
  implementation(project(":modules:client-profile"))
  implementation(project(":modules:case-mgmt"))
  implementation(project(":modules:program-enrollment"))
  implementation(project(":modules:incident-tracking"))
  implementation(project(":modules:user-access"))
  implementation(project(":modules:reporting"))
  implementation(project(":shared-kernel"))
  implementation(project(":event-store"))

  // Spring Boot Starters
  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.boot:spring-boot-starter-data-jpa")
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-client")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-validation")
  implementation("org.springframework.boot:spring-boot-starter-cache")
  implementation("org.springframework.boot:spring-boot-starter-mail")
  
  // Database & Migration
  runtimeOnly("org.postgresql:postgresql")
  implementation("org.flywaydb:flyway-core")
  implementation("org.flywaydb:flyway-database-postgresql")
  
  // Caching
  implementation("org.springframework.boot:spring-boot-starter-cache")
  implementation("org.ehcache:ehcache:3.10.8")
  implementation("javax.cache:cache-api")
  implementation("org.hibernate.orm:hibernate-jcache:6.6.18.Final")
  
  // API Documentation
  implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")
  
  // Kafka (Optional)
  implementation("org.springframework.kafka:spring-kafka")
  
  // Utilities
  implementation("org.apache.commons:commons-lang3:3.14.0")
  implementation("commons-io:commons-io:2.15.1")
  implementation("com.google.guava:guava:33.0.0-jre")
  
  // Development Tools
  developmentOnly("org.springframework.boot:spring-boot-devtools")
  runtimeOnly("com.h2database:h2")
  
  // Lombok (Optional but useful)
  compileOnly("org.projectlombok:lombok")
  annotationProcessor("org.projectlombok:lombok")
  
  // Testing
  testImplementation("org.springframework.boot:spring-boot-starter-test")
  testImplementation("org.springframework.security:spring-security-test")
  testImplementation("org.mockito:mockito-junit-jupiter")
  testImplementation("org.testcontainers:testcontainers:1.19.3")
  testImplementation("org.testcontainers:postgresql:1.19.3")
  testImplementation("org.testcontainers:junit-jupiter:1.19.3")
  testImplementation("io.rest-assured:rest-assured")
}

// Flyway configuration
flyway {
    url = "jdbc:postgresql://localhost:5432/haven"
    user = "haven"
    password = "haven"
    locations = arrayOf("classpath:db/migration")
}