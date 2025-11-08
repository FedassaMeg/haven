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
    // Internal dependencies
    api(project(":shared-kernel"))
    api(project(":event-store"))
    api(project(":modules:client-profile"))

    // Spring Boot starters
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-web")

    // Spring Framework
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-tx")

    // JSON handling
    implementation("com.fasterxml.jackson.core:jackson-databind")

    // PostgreSQL JDBC
    implementation("org.postgresql:postgresql")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito:mockito-junit-jupiter")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
