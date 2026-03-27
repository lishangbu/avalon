import org.gradle.api.tasks.PathSensitivity

plugins {
    `java-library`
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.dokka)
}

val mainDtoDir = layout.projectDirectory.dir("src/main/dto")

dependencies {
    api(project(":avalon-platform:avalon-jimmer"))
    api(libs.jimmer.sql.kotlin)
    api(libs.jackson.databind)
    ksp(libs.jimmer.ksp)
}

tasks.configureEach {
    if (name == "kspKotlin") {
        // Make DTO definitions part of the cache key for clean and remote builds.
        inputs
            .dir(mainDtoDir)
            .withPropertyName("jimmerMainDtoDir")
            .withPathSensitivity(PathSensitivity.RELATIVE)
    }
}
