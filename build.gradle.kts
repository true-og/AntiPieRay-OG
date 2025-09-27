import java.io.BufferedReader

plugins {
    kotlin("jvm") version "2.2.20"
    id("com.diffplug.spotless") version "7.0.4"
    id("com.gradleup.shadow") version "8.3.6"
}

val commitHash =
    try {
        Runtime.getRuntime().exec(arrayOf("git", "rev-parse", "--short=10", "HEAD")).let { process ->
            process.waitFor()
            val output = process.inputStream.use { it.bufferedReader().use(BufferedReader::readText) }
            process.destroy()
            output.trim().ifBlank { "unknown" }
        }
    } catch (_: Exception) {
        "unknown"
    }

val apiVersion = "1.19"

group = "net.trueog"

version = "$apiVersion-$commitHash"

repositories {
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/") { name = "papermc-repo" }
    maven("https://repo.codemc.io/repository/maven-releases/") { name = "codemc-repo" }
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.19.4-R0.1-SNAPSHOT")
    implementation("com.github.retrooper:packetevents-spigot:2.9.5")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
}

tasks {
    build {
        dependsOn(spotlessApply)
        dependsOn(shadowJar)
    }
    jar { archiveClassifier.set("part") }
    shadowJar { archiveClassifier.set("") }
}

tasks.processResources {
    val props = mapOf("version" to version, "apiVersion" to apiVersion)
    inputs.properties(props)
    filteringCharset = "UTF-8"
    filesMatching("plugin.yml") { expand(props) }
}

val targetJavaVersion = 17

kotlin { jvmToolchain(targetJavaVersion) }

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
        vendor = JvmVendorSpec.GRAAL_VM
    }
}

tasks.withType<AbstractArchiveTask>().configureEach {
    isPreserveFileTimestamps = false
    isReproducibleFileOrder = true
}

spotless {
    kotlin { ktfmt().kotlinlangStyle().configure { it.setMaxWidth(120) } }
    kotlinGradle {
        ktfmt().kotlinlangStyle().configure { it.setMaxWidth(120) }
        target("build.gradle.kts", "settings.gradle.kts")
    }
}
