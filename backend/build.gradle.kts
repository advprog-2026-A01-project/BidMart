import org.gradle.api.plugins.quality.Pmd
import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.Copy

plugins {
    java
    id("org.springframework.boot") version "3.5.11"
    id("io.spring.dependency-management") version "1.1.7"
    pmd
}

group = "id.ac.ui.cs.advprog"
version = "0.0.1-SNAPSHOT"
description = "backend"

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom(configurations.annotationProcessor.get())
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-web")

    // auth + db
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-jdbc")
    runtimeOnly("org.postgresql:postgresql")

    compileOnly("org.projectlombok:lombok")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")

    // add: in-memory DB for tests
    testRuntimeOnly("com.h2database:h2")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

pmd {
    // jangan pakai ruleset default Gradle
    ruleSets = emptyList()
    isConsoleOutput = true

    // penting untuk Java 21 (classfile major 65)
    toolVersion = "7.11.0"
}

// set ignoreFailures di task PMD (bukan di extension pmd {})
tasks.withType<Pmd>().configureEach {
    ignoreFailures = false
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.named<Pmd>("pmdMain") {
    ruleSetFiles = files("$rootDir/config/pmd/ruleset.xml")
}

tasks.named<Pmd>("pmdTest") {
    ruleSetFiles = files("$rootDir/config/pmd/ruleset-test.xml")
}

val frontendDir = file("../frontend")

val npmInstall by tasks.registering(Exec::class) {
    workingDir = frontendDir
    commandLine("npm", "ci")
}

val npmBuild by tasks.registering(Exec::class) {
    workingDir = frontendDir
    dependsOn(npmInstall)
    commandLine("npm", "run", "build")
}

val copyFrontendToStatic by tasks.registering(Copy::class) {
    dependsOn(npmBuild)
    from(file("$frontendDir/dist"))
    into(file("$projectDir/src/main/resources/static"))
}

tasks.named("processResources") {
    dependsOn(copyFrontendToStatic)
}

tasks.named("bootJar") {
    dependsOn(copyFrontendToStatic)
}
