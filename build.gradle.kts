import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    alias(libs.plugins.shadow)
}

group = "net.azisaba.yukitexture"
version = "3.1.0-1.21.1"

java.toolchain.languageVersion.set(JavaLanguageVersion.of(21))

repositories {
    mavenCentral()
    maven("https://jitpack.io/")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://rayzr.dev/repo/")
}

dependencies {
    implementation(kotlin("stdlib"))
    compileOnly(libs.paper.api)
    implementation(libs.kaml)
//    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    implementation("redis.clients:jedis:5.2.0")
    implementation("com.github.kittinunf.fuel:fuel:2.2.3")
    implementation("commons-codec:commons-codec:1.15")
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_21
    }
}

tasks.shadowJar {
    relocationPrefix = "net.azisaba.yukitexture.libs"
    minimize()

    // === Include & Exclude ===
    // JetBrains annotations should not be included in jar
    exclude("org.jetbrains.annotations")
}

shadow {
}
