import org.gradle.api.plugins.quality.Pmd

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

    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    runtimeOnly("com.h2database:h2")

    compileOnly("org.projectlombok:lombok")
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
    annotationProcessor("org.projectlombok:lombok")
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

pmd {
    // jangan pakai ruleset default Gradle
    ruleSets = emptyList()
    isConsoleOutput = true
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
