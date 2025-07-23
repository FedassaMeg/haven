plugins { `java-library` }

dependencies {
  api(platform(project(":platform-bom")))
  api("org.springframework:spring-context")
  api("com.fasterxml.jackson.core:jackson-annotations")
}