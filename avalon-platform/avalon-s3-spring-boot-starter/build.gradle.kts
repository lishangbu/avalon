plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.dokka)
}

dependencies {
    api(libs.spring.boot.autoconfigure)
    api(libs.aws.s3)
    kapt(libs.spring.boot.configuration.processor)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.minio)
}
