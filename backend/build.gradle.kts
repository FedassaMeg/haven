plugins {
  `java-platform`
  id("org.springframework.boot") version "3.3.1" apply false
  id("io.spring.dependency-management") version "1.1.5" apply false
  kotlin("jvm") version "1.9.24" apply false // if using kotlin
}

javaPlatform.allowDependencies()

subprojects {
  apply(plugin = "java-library")
  repositories {
    mavenCentral()
  }
  group = "org.haven"
  version = "0.1.0-SNAPSHOT"

  tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(21) // Java 21 LTS recommended
  }

  tasks.withType<Test> {
    useJUnitPlatform()
  }
}