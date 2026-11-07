plugins {
    kotlin("jvm") version "2.0.21"
    id("com.gradleup.shadow") version "8.3.0"
}

group = "net.azisaba"
version = "3.0.5+1.21.11"

java.toolchain.languageVersion.set(JavaLanguageVersion.of(21))

repositories {
    mavenCentral()
    maven("https://jitpack.io/")
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://rayzr.dev/repo/")
    maven("https://repo.viaversion.com/")
}

dependencies {
    implementation(kotlin("stdlib"))
    compileOnly("io.papermc.paper:paper-api:1.21.11-R0.1-SNAPSHOT")
    implementation("redis.clients:jedis:8.0.1")
    compileOnly("com.viaversion:viaversion-api:4.9.3")
    testImplementation(kotlin("test"))
}

tasks {
    build {
        dependsOn(shadowJar)
    }

    withType<JavaCompile> {
        options.encoding = "UTF-8"
    }

    processResources {
        filesMatching("plugin.yml") {
            expand("version" to project.version)
        }
    }


    jar {
        enabled = false
    }

    shadowJar {
        archiveClassifier.set("")
        // JetBrains annotations should not be included in jar
        exclude("org.jetbrains.annotations")
        relocate("kotlin", "net.azisaba.yukitexture.libs.kotlin")
        relocate("redis.clients", "net.azisaba.yukitexture.libs.redis.clients")
    }
}
