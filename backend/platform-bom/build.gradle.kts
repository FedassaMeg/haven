plugins {
    `java-platform`
}

javaPlatform {
    allowDependencies()
}

val springBootVersion = "3.3.1"
val jacksonVersion = "2.17.1"
val postgresVersion = "42.7.3"
val junitJupiterVersion = "5.10.2"

dependencies {
    constraints {
        api("org.springframework.boot:spring-boot-starter-validation:$springBootVersion")
        api("org.springframework.boot:spring-boot-starter-json:$springBootVersion")
        api("org.springframework:spring-context:6.1.6")
        api("org.springframework:spring-jdbc:6.1.6")
        api("com.fasterxml.jackson.core:jackson-annotations:$jacksonVersion")
        api("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
        api("org.postgresql:postgresql:$postgresVersion")
        api("org.junit.jupiter:junit-jupiter:$junitJupiterVersion")
    }
}

