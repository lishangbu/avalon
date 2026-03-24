plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.dokka)
}

dependencies {
    api(project(":avalon-support:avalon-web-support"))
    compileOnly(libs.slf4j.api)
    api(libs.spring.boot.autoconfigure)
    api(libs.spring.security.core)
    api(libs.spring.security.oauth2.core)
    api(libs.spring.security.web)
    compileOnly(libs.jakarta.servlet.api)
}
