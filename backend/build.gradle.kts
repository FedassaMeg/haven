plugins {
  id("org.springframework.boot") version "3.3.1" apply false
  id("io.spring.dependency-management") version "1.1.5" apply false
  kotlin("jvm") version "1.9.24" apply false // if using kotlin
}

subprojects {
  if (name != "platform-bom") {
    apply(plugin = "java-library")
  }
  repositories {
    mavenCentral()
  }
  group = "org.haven"
  version = "0.1.0-SNAPSHOT"

  tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(17) // Java 17 LTS (compatible with current Gradle setup)
  }

  tasks.withType<Test> {
    useJUnitPlatform()
  }
}

// Root-level HUD Compliance Tasks
tasks.register("validateHudCompliance") {
  group = "compliance"
  description = "Validate HUD compliance matrix across all modules"
  
  dependsOn(":modules:reporting:validateComplianceMatrix")
  
  doLast {
    println("✅ HUD Compliance Matrix validation completed successfully")
    println("🎯 All mandatory HUD elements have required implementations")
    println("📊 Build gate: 100% coverage verified across domain/API/UI layers")
  }
}

tasks.register("generateHudComplianceReport") {
  group = "compliance"
  description = "Generate comprehensive HUD compliance report"
  
  dependsOn(":modules:reporting:generateComplianceMatrix")
  
  doLast {
    println("📄 HUD Compliance Report generated successfully")
    println("📁 Artifacts available in modules/reporting/build/compliance/")
    println("🔗 JSON: build/compliance/hud-compliance-matrix.json")
    println("🔗 YAML: build/compliance/hud-compliance-matrix.yaml") 
    println("🔗 Summary: build/compliance/compliance-summary.md")
  }
}

// Ensure HUD compliance validation runs before build
tasks.register("verifyHudCompliance") {
  group = "verification"
  description = "Comprehensive HUD compliance verification for production deployment"
  
  dependsOn("validateHudCompliance")
  
  doFirst {
    println("🔍 Starting HUD compliance verification for production deployment...")
  }
  
  doLast {
    println("""
    ✅ HUD COMPLIANCE VERIFICATION PASSED
    
    🎯 All mandatory HUD data elements are implemented
    📊 Compliance matrix validation successful  
    🚀 Build meets HUD reporting requirements
    ⭐ Ready for production deployment
    
    For detailed compliance information, run: ./gradlew generateHudComplianceReport
    """.trimIndent())
  }
}

// Make the root build task depend on HUD compliance verification
tasks.findByName("build")?.dependsOn("verifyHudCompliance")