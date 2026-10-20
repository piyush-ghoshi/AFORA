import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    id("org.springframework.boot") version "3.2.0"
    id("io.spring.dependency-management") version "1.1.4"
    kotlin("jvm") version "1.9.22"
    kotlin("plugin.spring") version "1.9.22"
    kotlin("plugin.jpa") version "1.9.22"
}

group = "com.afora"
version = "1.0.0"
java.sourceCompatibility = JavaVersion.VERSION_17

repositories {
    mavenCentral()
}

dependencies {
    // Spring Boot Starters
    implementation("org.springframework.boot:spring-boot-starter-web")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    
    // Kotlin
    implementation("org.jetbrains.kotlin:kotlin-reflect")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
    implementation("com.fasterxml.jackson.module:jackson-module-kotlin")
    
    // Firebase Admin SDK for authentication
    implementation("com.google.firebase:firebase-admin:9.2.0")
    
    // PostgreSQL
    runtimeOnly("org.postgresql:postgresql")
    
    // Flyway for DB migrations
    implementation("org.flywaydb:flyway-core")
    
    // OpenAPI / Swagger UI
    implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:2.3.0")
    
    // Development
    developmentOnly("org.springframework.boot:spring-boot-devtools")
    
    // Testing
    testImplementation("org.springframework.boot:spring-boot-starter-test")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("com.h2database:h2")
    testImplementation("org.jetbrains.kotlin:kotlin-test")
    // Property testing: library only, NOT the Kotest runner, so JUnit suites keep running
    testImplementation("io.kotest:kotest-property-jvm:5.8.0")
    // kotest-property's checkAll is a suspend function; kotlinx-coroutines-core is already
    // pulled in transitively (runtime scope) by kotest-property/mockk-dsl, this just makes
    // the matching version visible on the test *compile* classpath so runBlocking resolves.
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-core:1.7.3")
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        freeCompilerArgs = listOf("-Xjsr305=strict")
        jvmTarget = "17"
    }
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.withType<org.springframework.boot.gradle.tasks.run.BootRun> {
    jvmArgs("-Djava.net.preferIPv4Stack=true")
}
