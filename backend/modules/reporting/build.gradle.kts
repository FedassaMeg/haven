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
    
    // Spring Framework
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-tx")
    
    // JPA/Hibernate
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    
    // Validation
    implementation("org.springframework.boot:spring-boot-starter-validation")
    
    // Reporting libraries (optional for future use)
    // implementation("net.sf.jasperreports:jasperreports:6.20.6")
    // implementation("org.apache.poi:poi:5.2.4")
    // implementation("org.apache.poi:poi-ooxml:5.2.4")
    
    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito:mockito-junit-jupiter")
}

tasks.withType<Test> {
    useJUnitPlatform()
}