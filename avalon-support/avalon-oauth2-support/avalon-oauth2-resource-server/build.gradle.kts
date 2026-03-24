plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.dokka)
}

dependencies {
    api(project(":avalon-support:avalon-oauth2-support:avalon-oauth2-common"))
    api(libs.spring.boot.starter.oauth2.resource.server)
    compileOnly(libs.jakarta.servlet.api)
    testRuntimeOnly(libs.jakarta.servlet.api)
}
