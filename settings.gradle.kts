rootProject.name = "AntiPieRay-OG"

// Run the bootstrap at configuration time.
val process = ProcessBuilder("sh", "bootstrap.sh")
    .directory(rootDir)
    .start()

val exitValue = process.waitFor()
if (exitValue != 0) {
    throw GradleException("bootstrap.sh failed with exit code $exitValue")
}

include("libs:Utilities-OG")

