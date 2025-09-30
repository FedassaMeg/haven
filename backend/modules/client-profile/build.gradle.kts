plugins {
    id("java-library")
    id("org.springframework.boot") apply false
    id("io.spring.dependency-management")
}

repositories {
    mavenCentral()
}

dependencyManagement {
    imports {
        mavenBom(org.springframework.boot.gradle.plugin.SpringBootPlugin.BOM_COORDINATES)
    }
}

dependencies {
    api(project(":shared-kernel"))
    api(project(":event-store"))
    
    // Spring Boot starters
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-security")
    
    // Spring Framework
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-tx")
    
    // JSON handling
    implementation("com.fasterxml.jackson.core:jackson-databind")

    // PostgreSQL JDBC
    implementation("org.postgresql:postgresql")

    // Hibernate Types no longer needed (using Hibernate 6 native enum mapping)
    
    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito:mockito-junit-jupiter")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
