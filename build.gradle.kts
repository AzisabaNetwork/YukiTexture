plugins {
    kotlin("jvm") version "1.7.21"
    id("com.gradleup.shadow") version "8.3.0"
}

group = "net.azisaba"
version = "3.0.4"

java.toolchain.languageVersion.set(JavaLanguageVersion.of(8))

repositories {
    mavenCentral()
    maven("https://jitpack.io/")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://rayzr.dev/repo/")
    maven("https://repo.viaversion.com/")
}

dependencies {
    implementation(kotlin("stdlib"))
    compileOnly("com.destroystokyo.paper:paper-api:1.16.5-R0.1-SNAPSHOT")
    implementation("redis.clients:jedis:4.3.2")
    compileOnly("com.viaversion:viaversion-api:4.9.3")
}

tasks {
    processResources {
        filesMatching("plugin.yml") {
            expand("version" to project.version)
        }
    }

    compileKotlin { kotlinOptions.jvmTarget = "1.8" }
    compileTestKotlin { kotlinOptions.jvmTarget = "1.8" }

    shadowJar {
        // JetBrains annotations should not be included in jar
        exclude("org.jetbrains.annotations")
        relocate("kotlin", "net.azisaba.yukitexture.libs.kotlin")
        relocate("redis.clients", "net.azisaba.yukitexture.libs.redis.clients")

        minimize()
    }
}
