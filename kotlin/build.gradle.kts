import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    kotlin("jvm") version "2.2.0"
    kotlin("plugin.serialization") version "2.2.0"
    application
    id("com.gradleup.shadow") version "9.4.1"
}

group = "com.buxfer"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

val ktorVersion = "3.2.0"
val mcpSdkVersion = "0.11.1"
val serializationVersion = "1.9.0"
val junitVersion = "5.14.4"
val mockkVersion = "1.14.9"
val coroutinesTestVersion = "1.10.2"
val assertjVersion = "3.27.7"
val jsonUnitVersion = "4.1.0"
val jacksonVersion = "2.19.0"

dependencies {
    // MCP server protocol
    implementation("io.modelcontextprotocol:kotlin-sdk:$mcpSdkVersion")

    // HTTP client for Buxfer API
    implementation("io.ktor:ktor-client-core:$ktorVersion")
    implementation("io.ktor:ktor-client-cio:$ktorVersion")
    implementation("io.ktor:ktor-client-content-negotiation:$ktorVersion")
    implementation("io.ktor:ktor-serialization-kotlinx-json:$ktorVersion")

    // JSON serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:$serializationVersion")

    // Logging — Logback classic. Writes to a rolling file via src/main/resources/logback.xml.
    // No ConsoleAppender in production: stdout is the MCP stdio transport and any output corrupts JSON-RPC frames.
    // Pulls in slf4j-api 2.0.x transitively; do not pin slf4j-api separately.
    implementation("ch.qos.logback:logback-classic:1.5.18")

    // Test
    testImplementation("org.junit.jupiter:junit-jupiter:$junitVersion")
    testImplementation("io.mockk:mockk:$mockkVersion")
    testImplementation("io.ktor:ktor-client-mock:$ktorVersion")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:$coroutinesTestVersion")
    testImplementation("org.assertj:assertj-core:$assertjVersion")
    testImplementation("net.javacrumbs.json-unit:json-unit-assertj:$jsonUnitVersion")
    testImplementation("com.fasterxml.jackson.core:jackson-databind:$jacksonVersion")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

application {
    mainClass.set("com.buxfer.mcp.MainKt")
}

kotlin {
    jvmToolchain(21)
}

tasks.withType<ShadowJar> {
    archiveClassifier.set("all")
    mergeServiceFiles()
}

tasks.named("build") {
    dependsOn("shadowJar")
}

tasks.test {
    useJUnitPlatform()
    // Shared anonymized fixtures used by all language implementations
    systemProperty("fixtures.dir", "${project.projectDir}/../shared/test-fixtures/responses")
}
