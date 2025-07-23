plugins { `java-library` }

dependencies {
  api(platform(project(":platform-bom")))
  api(project(":shared-kernel"))
  implementation("org.springframework:spring-jdbc")
  implementation("org.postgresql:postgresql")
  implementation("com.fasterxml.jackson.core:jackson-databind")
  testImplementation("org.junit.jupiter:junit-jupiter")
}