plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.dokka)
}

dependencies {
    api(project(":avalon-modules:avalon-game:avalon-game-service"))
    api(project(":avalon-modules:avalon-game:avalon-game-player"))
    api(project(":avalon-platform:avalon-web"))
}
