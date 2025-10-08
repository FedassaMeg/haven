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
    api(project(":modules:client-profile"))
    api(project(":modules:program-enrollment"))
    
    // Spring Framework
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-tx")
    
    // JPA/Hibernate
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    
    // Validation
    implementation("org.springframework.boot:spring-boot-starter-validation")
    
    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito:mockito-junit-jupiter")
}

tasks.withType<Test> {
    useJUnitPlatform()
}
