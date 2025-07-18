plugins {
  `java-library`
}

dependencies {
  api(project(":shared-kernel"))
  api(project(":event-store"))
  implementation("org.springframework.boot:spring-boot-starter-validation")
  implementation("org.springframework.boot:spring-boot-starter-json")
  testImplementation("org.springframework.boot:spring-boot-starter-test")
}