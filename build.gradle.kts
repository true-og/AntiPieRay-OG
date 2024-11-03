plugins {
    java
    eclipse
    id("com.gradleup.shadow") version "8.3.2"
    id("io.papermc.paperweight.userdev") version "1.7.2"
    id("xyz.jpenilla.run-paper") version "2.3.0"
    id("net.kyori.indra.git") version "2.0.0"
}

group = "net.trueog.anti-pieray-og"
version = "1.0"
val apiVersion = "1.19"

repositories {
    mavenCentral()
    gradlePluginPortal()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://repo.jpenilla.xyz/snapshots/")
    maven("https://repo.essentialsx.net/releases/")
    maven("https://repo.spongepowered.org/repository/maven-public/")
    maven("https://repo.purpurmc.org/snapshots")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/") {
        content { includeGroup("me.clip") }
    }
    maven("https://jitpack.io") {
        content { includeGroupByRegex("com\\.github\\..*") }
    }
}

dependencies {
    // Paperweight development bundle for Minecraft 1.19.4
    paperweight.paperDevBundle("1.19.4-R0.1-SNAPSHOT")

    compileOnly("org.purpurmc.purpur:purpur-api:1.19.4-R0.1-SNAPSHOT")
    compileOnly("net.orbyfied.j8:j8-util:0.2.2.1")

    implementation("xyz.jpenilla:reflection-remapper:0.1.1")
    implementation(platform("org.incendo:cloud-bom:2.0.0-rc.2"))
    implementation(platform("org.incendo:cloud-minecraft-bom:2.0.0-beta.9"))
    implementation("org.incendo:cloud-paper")
    implementation("org.incendo:cloud-minecraft-extras")
    implementation("io.papermc:paperlib:1.0.8")
    implementation(project(":libs:Utilities-OG"))
}

tasks {
    compileJava {
        options.release.set(17)
    }

    named<ProcessResources>("processResources") {
        val props = mapOf(
            "version" to version,
            "apiVersion" to apiVersion
        )
        inputs.properties(props)
        filesMatching("plugin.yml") {
            expand(props)
        }
    }

    shadowJar {
        archiveClassifier.set("")
        from("LICENSE") { into("/") }
        minimize()

        val prefix = "${project.group}.${project.name.lowercase()}.lib"

        // Adjusted relocations for dependencies
        relocate("com.typesafe.config", "$prefix.com.typesafe.config")
        relocate("io.leangen.geantyref", "$prefix.io.leangen.geantyref")
        relocate("io.papermc.lib", "$prefix.io.papermc.lib")

        // Exclude net.kyori.adventure from relocation
        relocate("net.kyori", "$prefix.net.kyori") {
            exclude("net/kyori/adventure/**")
        }

        relocate("org.incendo", "$prefix.org.incendo")
        relocate("xyz.jpenilla.reflectionremapper", "$prefix.xyz.jpenilla.reflectionremapper")

        dependencies {
            exclude(dependency("org.jetbrains:annotations"))
        }
    }

    named("build") {
        dependsOn("shadowJar")
    }

    jar {
        archiveClassifier.set("part")
    }

    reobfJar {
        outputJar.set(layout.buildDirectory.file("libs/AntiPieRay-${project.version}.jar"))
    }
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(17)
        vendor = JvmVendorSpec.GRAAL_VM
    }
}

fun String.decorateVersion(): String =
    if (endsWith("-SNAPSHOT")) "$this+${lastCommitHash()}" else this

fun lastCommitHash(): String = indraGit.commit()?.name?.substring(0, 7)
    ?: error("Failed to determine git hash.")

