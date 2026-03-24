import org.springframework.boot.gradle.tasks.bundling.BootBuildImage

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

tasks.named<BootBuildImage>("bootBuildImage") {
    val repository = providers.gradleProperty("dockerRepository").orElse("docker.io")
    val prefix = providers.gradleProperty("dockerImagePrefix").orElse("slf4j")
    imageName.set("${repository.get()}/${prefix.get()}/${project.name}:latest")
    publish.set(false)
}

tasks.processResources {
    from(rootProject.layout.projectDirectory.dir("db")) {
        into("db")
    }
}
