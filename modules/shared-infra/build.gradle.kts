plugins {
    `java-library`
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.allopen")
    alias(libs.plugins.jandex)
}

dependencies {
    api(project(":modules:shared-kernel"))
    api(project(":modules:shared-application"))
    implementation(platform(libs.quarkus.bom))
    implementation(libs.quarkus.arc)
    implementation(libs.quarkus.kotlin)
    implementation(libs.quarkus.reactive.pg.client)
    implementation(libs.quarkus.scheduler)
    implementation(libs.kotlinx.coroutines.jdk8)
}
