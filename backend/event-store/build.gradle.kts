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
    implementation("org.springframework:spring-jdbc")
    implementation("org.springframework:spring-tx")
    implementation("org.springframework:spring-context")
    
    // Database
    implementation("org.postgresql:postgresql")
    
    // JSON handling
    implementation("com.fasterxml.jackson.core:jackson-databind")
    
    // Testing
    testImplementation("org.junit.jupiter:junit-jupiter")
    testImplementation("org.mockito:mockito-core")
    testImplementation("org.mockito:mockito-junit-jupiter")
    testImplementation("org.springframework:spring-test")
}

tasks.withType<Test> {
    useJUnitPlatform()
}