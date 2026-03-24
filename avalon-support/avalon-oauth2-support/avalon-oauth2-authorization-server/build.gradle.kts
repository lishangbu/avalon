plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.dokka)
}

dependencies {
    api(project(":avalon-support:avalon-oauth2-support:avalon-oauth2-resource-server"))
    api(libs.spring.boot.autoconfigure)
    api(libs.spring.boot.starter.oauth2.authorization.server)
    api(libs.spring.boot.starter.oauth2.client)
}
