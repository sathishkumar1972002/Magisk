plugins {
    id("MagiskPlugin")
    kotlin("android") version "1.9.22"
}

tasks.register("clean", Delete::class) {
    delete(rootProject.layout.buildDirectory)
}
