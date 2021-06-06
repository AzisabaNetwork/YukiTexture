import java.util.*

plugins {
    kotlin("jvm") version "1.5.10"
    id("com.github.johnrengelman.shadow") version "6.0.0"
}

group = "net.azisaba"
version = "1.0.1"

repositories {
    mavenCentral()
    maven("https://papermc.io/repo/repository/maven-public/")
    maven("https://www.jitpack.io")
    maven("https://rayzr.dev/repo/")
}

dependencies {
    implementation(kotlin("stdlib"))
    compileOnly("com.destroystokyo.paper:paper-api:1.12.2-R0.1-SNAPSHOT")
    implementation("org.jooq:joor-java-8:0.9.13")
    implementation("com.github.kittinunf.fuel:fuel:2.2.3")
    implementation("me.rayzr522:jsonmessage:1.2.1")
    implementation("commons-codec:commons-codec:1.15")
    implementation("org.mariadb.jdbc:mariadb-java-client:2.6.0")
}

tasks {
    compileKotlin { kotlinOptions.jvmTarget = "1.8" }
    compileTestKotlin { kotlinOptions.jvmTarget = "1.8" }

    shadowJar {
        relocate("kotlin", UUID.randomUUID().toString())
        relocate("org.jetbrains.annotations", UUID.randomUUID().toString())
        relocate("org.joor", UUID.randomUUID().toString())
        relocate("com.github.kittinunf.fuel", UUID.randomUUID().toString())
        relocate("com.github.kittinunf.result", UUID.randomUUID().toString())
        relocate("me.rayzr522.jsonmessage", UUID.randomUUID().toString())
        relocate("org.apache.commons.codec", UUID.randomUUID().toString())
        relocate("org.mariadb.jdbc", UUID.randomUUID().toString())

        minimize()
    }
}
