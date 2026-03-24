plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.dokka)
}

dependencies {
    api(libs.spring.boot.autoconfigure)
    testImplementation(libs.spring.boot.jackson)
}
