plugins {
  id("org.springframework.boot")
  id("io.spring.dependency-management")
  java
}

dependencies {
  implementation(project(":modules:client-profile"))
  implementation(project(":modules:case-mgmt"))
  implementation(project(":modules:program-enrollment"))
  implementation(project(":modules:incident-tracking"))
  implementation(project(":modules:user-access"))
  implementation(project(":modules:reporting"))

  implementation("org.springframework.boot:spring-boot-starter-web")
  implementation("org.springframework.boot:spring-boot-starter-actuator")
  implementation("org.springframework.boot:spring-boot-starter-oauth2-resource-server")
  implementation("org.springframework.boot:spring-boot-starter-security")
  implementation("org.springframework.boot:spring-boot-starter-validation")
  runtimeOnly("org.postgresql:postgresql")

  testImplementation("org.springframework.boot:spring-boot-starter-test")
}