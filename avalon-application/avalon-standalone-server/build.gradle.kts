plugins {
    java
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.dokka)
    alias(libs.plugins.spring.boot)
}

dependencies {
    implementation(project(":avalon-modules:avalon-authorization"))
    implementation(project(":avalon-modules:avalon-dataset:avalon-dataset-repository"))
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.spring.boot.starter.web)
    implementation(libs.jimmer.spring.boot.starter)
    runtimeOnly(libs.postgresql)
}

tasks.processResources {
    from(rootProject.layout.projectDirectory.dir("db")) {
        into("db")
    }
}
