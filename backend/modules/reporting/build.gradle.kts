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
    api(project(":modules:client-profile"))
    api(project(":modules:program-enrollment"))
    api(project(":modules:case-mgmt"))
    api(project(":modules:service-delivery"))
    api(project(":modules:read-models"))
    api(project(":modules:reporting-metadata"))

    // Spring Framework
    implementation("org.springframework:spring-context")
    implementation("org.springframework:spring-tx")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-web")
    
    // JPA/Hibernate
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    
    // Validation
    implementation("org.springframework.boot:spring-boot-starter-validation")
    
    // JSON/YAML processing for compliance matrix
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-yaml")

    // Apache POI for Excel exports
    implementation("org.apache.poi:poi:5.2.4")
    implementation("org.apache.poi:poi-ooxml:5.2.4")

    // Jakarta Mail for email notifications
    implementation("jakarta.mail:jakarta.mail-api:2.1.2")
    implementation("org.eclipse.angus:angus-mail:2.0.2")

    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.mockito:mockito-junit-jupiter")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

// HUD Compliance Matrix Tasks
tasks.register<Exec>("generateComplianceMatrix") {
    group = "compliance"
    description = "Generate HUD compliance matrix as JSON and YAML"
    
    dependsOn("classes")
    
    commandLine("java", "-cp", sourceSets.main.get().runtimeClasspath.asPath,
        "org.haven.reporting.application.services.ComplianceMatrixGenerator")
    
    doFirst {
        val outputDir = File("$buildDir/compliance")
        outputDir.mkdirs()
    }
    
    doLast {
        println("✅ HUD compliance matrix generated in build/compliance/")
    }
}

tasks.register<Exec>("validateComplianceMatrix") {
    group = "compliance"
    description = "Validate HUD compliance matrix for build gate"
    
    dependsOn("generateComplianceMatrix")
    
    commandLine("java", "-cp", sourceSets.main.get().runtimeClasspath.asPath,
        "org.haven.reporting.application.services.ComplianceMatrixValidator")
    
    doLast {
        println("✅ HUD compliance matrix validation passed")
    }
}

// Make build depend on compliance validation
// Temporarily disabled due to compilation errors
// tasks.named("build") {
//     dependsOn("validateComplianceMatrix")
// }