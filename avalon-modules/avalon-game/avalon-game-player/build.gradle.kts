plugins {
    `java-library`
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.dokka)
}

dependencies {
    api(project(":avalon-modules:avalon-game:avalon-game-battle-engine"))
    implementation(project(":avalon-platform:avalon-jimmer"))
    implementation(project(":avalon-modules:avalon-dataset:avalon-dataset-service"))
    implementation(libs.jimmer.spring.boot.starter)
    implementation(libs.jimmer.sql.kotlin)
    implementation(libs.spring.boot.autoconfigure)
    ksp(libs.jimmer.ksp)
}
