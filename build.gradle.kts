plugins {
    kotlin("jvm") version "1.7.21"
    id("com.github.johnrengelman.shadow") version "7.1.2"
}

group = "net.azisaba"
version = "3.0.0"

java.toolchain.languageVersion.set(JavaLanguageVersion.of(8))

repositories {
    mavenCentral()
    maven("https://jitpack.io/")
    maven("https://papermc.io/repo/repository/maven-public/")
    maven("https://rayzr.dev/repo/")
}

dependencies {
    implementation(kotlin("stdlib"))
    compileOnly("com.destroystokyo.paper:paper-api:1.16.5-R0.1-SNAPSHOT")
    implementation("redis.clients:jedis:4.3.2")
    implementation("com.github.kittinunf.fuel:fuel:2.2.3")
    implementation("commons-codec:commons-codec:1.15")
}

tasks {
    compileKotlin { kotlinOptions.jvmTarget = "1.8" }
    compileTestKotlin { kotlinOptions.jvmTarget = "1.8" }

    shadowJar {
        // JetBrains annotations should not be included in jar
        exclude("org.jetbrains.annotations")
        relocate("kotlin", "net.azisaba.yukitexture.libs.kotlin")
        relocate("com.github.kittinunf.fuel", "net.azisaba.yukitexture.libs.com.github.kittinunf.fuel")
        relocate("com.github.kittinunf.result", "net.azisaba.yukitexture.libs.com.github.kittinunf.result")
        relocate("org.apache.commons.codec", "net.azisaba.yukitexture.libs.org.apache.commons.codec")
        relocate("redis.clients", "net.azisaba.yukitexture.libs.redis.clients")

        minimize()
    }
}
