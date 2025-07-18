plugins { `java-library` }

dependencies {
  api(project(":shared-kernel"))
  implementation("org.springframework:spring-jdbc")
  implementation("org.postgresql:postgresql")
  implementation("com.fasterxml.jackson.core:jackson-databind")
  testImplementation("org.junit.jupiter:junit-jupiter")
}