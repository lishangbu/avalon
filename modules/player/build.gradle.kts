plugins {
    `java-library`
    id("org.jetbrains.kotlin.jvm")
}

dependencies {
    api(project(":modules:shared-kernel"))
    implementation(project(":modules:shared-infra"))
}
