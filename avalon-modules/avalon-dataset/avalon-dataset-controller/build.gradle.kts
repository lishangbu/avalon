plugins {
    `java-library`
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.spring")
    id("org.jetbrains.dokka")
}

dependencies {
    api(project(":avalon-modules:avalon-dataset:avalon-dataset-service"))
    api(project(":avalon-support:avalon-web-support"))
}
