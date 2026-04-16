plugins {
    `java-library`
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.allopen")
    alias(libs.plugins.jandex)
}

dependencies {
    api(project(":modules:shared-kernel"))
    implementation(project(":modules:shared-application"))
    implementation(platform(libs.quarkus.bom))
    implementation(project(":modules:shared-infra"))
    implementation(libs.quarkus.arc)
    implementation(libs.quarkus.kotlin)
    implementation(libs.quarkus.reactive.pg.client)
    implementation(libs.quarkus.rest)
    implementation(libs.quarkus.rest.jackson)
    implementation(libs.quarkus.hibernate.validator)
    implementation(libs.kotlinx.coroutines.jdk8)
}
