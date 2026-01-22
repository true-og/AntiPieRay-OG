import java.io.BufferedReader

plugins {
    eclipse
    kotlin("jvm") version "2.1.21"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.19"
    id("io.papermc.paperweight.userdev") version "2.0.0-beta.18"
    id("com.diffplug.spotless") version "8.1.0"
    id("com.gradleup.shadow") version "8.3.9"
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
    paperweight.paperDevBundle("1.19.4-R0.1-SNAPSHOT")

    compileOnly("io.papermc.paper:paper-api:1.19.4-R0.1-SNAPSHOT")
    implementation("com.github.retrooper:packetevents-spigot:2.9.5")
    implementation("org.jetbrains.kotlin:kotlin-stdlib-jdk8")
}

paperweight.reobfArtifactConfiguration = io.papermc.paperweight.userdev.ReobfArtifactConfiguration.REOBF_PRODUCTION

tasks {
    assemble { dependsOn(reobfJar) }
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
