plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.dokka)
}

dependencies {
    api(libs.spring.boot.starter.json)
    compileOnly(libs.slf4j.api)
    api(libs.spring.webmvc)
    compileOnly(libs.jakarta.servlet.api)
}
