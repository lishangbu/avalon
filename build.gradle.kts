import io.github.lishangbu.avalon.gradle.support.configureAvalonJvmConventions
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.register

plugins {
    base
    id("org.jetbrains.kotlin.jvm") apply false
    id("org.jetbrains.kotlin.plugin.allopen") apply false
    alias(libs.plugins.quarkus) apply false
}

group = "io.github.lishangbu"
version = "0.0.1-SNAPSHOT"

abstract class PrintVersionTask : DefaultTask() {
    @get:Input
    abstract val versionText: Property<String>

    @TaskAction
    fun printVersion() {
        println(versionText.get())
    }
}

subprojects {
    group = rootProject.group
    version = rootProject.version

    configureAvalonJvmConventions()
}

tasks.register<PrintVersionTask>("printVersion") {
    group = "build setup"
    description = "Print the version of this project."
    versionText.set(version.toString())
}
