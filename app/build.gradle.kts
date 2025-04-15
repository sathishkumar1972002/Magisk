plugins {
    kotlin("android") version "1.9.22"
}
tasks.register("clean") {
    subprojects.forEach {
        dependsOn(":app:${it.name}:clean")
    }
}
