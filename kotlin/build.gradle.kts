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

    // Suppress SLF4J output to stdout (would break stdio MCP transport)
    implementation("org.slf4j:slf4j-nop:2.0.17")
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
