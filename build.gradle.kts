plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.shadow)
    id("xyz.jpenilla.run-paper") version "2.3.1"
}

group = "net.azisaba.yukitexture"
version = "3.1.0-SNAPSHOT"

repositories {
    mavenCentral()
    maven("https://jitpack.io/")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://rayzr.dev/repo/")
    maven("https://repo.eclipse.org/content/groups/releases/") {
        name = "eclipse-repo"
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    compileOnly(libs.paper.api)
    implementation(libs.kaml)
    implementation(awssdk.services.s3)
    implementation("redis.clients:jedis:5.2.0")
    implementation("com.github.kittinunf.fuel:fuel:2.2.3")
    implementation("commons-codec:commons-codec:1.15")
    implementation("org.eclipse.jgit:org.eclipse.jgit:7.2.0.202502191417-m3")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.8.0")
}

kotlin {
    jvmToolchain(21)
}

tasks.shadowJar {
    relocationPrefix = "net.azisaba.yukitexture.libs"
    minimize()

    // === Include & Exclude ===
    // JetBrains annotations should not be included in jar
    exclude("org.jetbrains.annotations")
}

tasks.runServer {
    minecraftVersion("1.21.1")
}
