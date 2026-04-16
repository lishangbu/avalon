import io.github.lishangbu.avalon.build.support.configureAvalonJvmConventions

plugins {
    base
    id("org.jetbrains.kotlin.jvm") apply false
    id("org.jetbrains.kotlin.plugin.allopen") apply false
    alias(libs.plugins.quarkus) apply false
}

group = "io.github.lishangbu"
version = "0.0.1-SNAPSHOT"

subprojects {
    group = rootProject.group
    version = rootProject.version

    configureAvalonJvmConventions()
}

tasks.register("printVersion") {
    group = "build setup"
    description = "Print the version of this project."
    doLast {
        println(project.version)
    }
}
