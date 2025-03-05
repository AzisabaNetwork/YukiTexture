plugins {
    kotlin("jvm") version "2.1.10"
    id("com.gradleup.shadow") version "9.0.0-beta8"
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
    compileOnly("io.papermc.paper:paper-api:1.21.1-R0.1-SNAPSHOT")
    implementation("redis.clients:jedis:5.2.0")
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
