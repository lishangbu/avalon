plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.dokka)
}

dependencies {
    api(libs.spring.boot.autoconfigure)
    api(libs.spring.boot.starter.data.redis)
    api(libs.spring.aop)
    implementation(libs.aspectjweaver)
    implementation(libs.spring.boot.starter.json)
    implementation(libs.jakarta.servlet.api)
    implementation(libs.spring.jdbc)
    implementation(libs.spring.web)
    kapt(libs.spring.boot.configuration.processor)
    testImplementation(libs.h2)
}
