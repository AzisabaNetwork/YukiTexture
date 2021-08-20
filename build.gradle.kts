plugins {
    kotlin("jvm") version "1.5.21"
    id("com.github.johnrengelman.shadow") version "6.0.0"
}

group = "net.azisaba"
version = "1.0.2"

repositories {
    mavenCentral()
    maven("https://papermc.io/repo/repository/maven-public/")
    maven("https://www.jitpack.io")
    maven("https://rayzr.dev/repo/")
}

dependencies {
    implementation(kotlin("stdlib"))
    compileOnly("com.destroystokyo.paper:paper-api:1.16.5-R0.1-SNAPSHOT")
    implementation("org.jooq:joor-java-8:0.9.13")
    implementation("com.github.kittinunf.fuel:fuel:2.2.3")
    implementation("me.rayzr522:jsonmessage:1.3.0")
    implementation("commons-codec:commons-codec:1.15")
    implementation("org.mariadb.jdbc:mariadb-java-client:2.6.0")
}

tasks {
    compileKotlin { kotlinOptions.jvmTarget = "1.8" }
    compileTestKotlin { kotlinOptions.jvmTarget = "1.8" }

    shadowJar {
        // JetBrains annotations should not be included in jar
        exclude("org.jetbrains.annotations")
        relocate("kotlin", "net.azisaba.yukitexture.libs.kotlin")
        relocate("org.joor", "net.azisaba.yukitexture.libs.org.joor")
        relocate("com.github.kittinunf.fuel", "net.azisaba.yukitexture.libs.com.github.kittinunf.fuel")
        relocate("com.github.kittinunf.result", "net.azisaba.yukitexture.libs.com.github.kittinunf.result")
        relocate("me.rayzr522.jsonmessage", "net.azisaba.yukitexture.libs.me.rayzr522.jsonmessage")
        relocate("org.apache.commons.codec", "net.azisaba.yukitexture.libs.org.apache.commons.codec")
        relocate("org.mariadb.jdbc", "net.azisaba.yukitexture.libs.org.mariadb.jdbc")

        minimize()
    }
}
