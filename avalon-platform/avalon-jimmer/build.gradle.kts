plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.dokka)
}

dependencies {
    implementation(libs.jimmer.core)
    implementation(libs.spring.boot.jackson)
    implementation(libs.spring.boot.autoconfigure)
}
