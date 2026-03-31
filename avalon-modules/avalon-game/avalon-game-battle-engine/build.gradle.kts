plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.dokka)
}

dependencies {
    api(project(":avalon-modules:avalon-game:avalon-game-calculator"))

    // JSON 序列化
    implementation(libs.spring.boot.starter.json)
    implementation(libs.spring.boot.autoconfigure)

    // 测试
    testImplementation(libs.spring.boot.starter.test)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.testcontainers.junit.jupiter)
}
