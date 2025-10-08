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
    implementation(project(":shared-kernel"))
    implementation(project(":event-store"))
    implementation(project(":modules:case-mgmt"))
    implementation(project(":modules:client-profile"))
    implementation(project(":modules:safety-assessment"))
    implementation(project(":modules:financial-assistance"))
    implementation(project(":modules:program-enrollment"))
    
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-cache")
    implementation("org.springframework.data:spring-data-redis")
    implementation("redis.clients:jedis")
    
    // Event handling
    implementation("org.axonframework:axon-spring-boot-starter:4.8.0")
    
    // Jackson for JSON serialization
    implementation("com.fasterxml.jackson.core:jackson-databind")
    implementation("com.fasterxml.jackson.datatype:jackson-datatype-jsr310")
    
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.testcontainers:postgresql:1.19.0")
    testImplementation("org.testcontainers:testcontainers:1.19.0")
}

tasks.test {
    useJUnitPlatform()
}
