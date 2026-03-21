import org.springframework.boot.gradle.tasks.bundling.BootBuildImage

plugins {
    java
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.spring")
    id("org.jetbrains.dokka")
    id("org.springframework.boot")
}

dependencies {
    implementation(project(":avalon-modules:avalon-authorization"))
    implementation(project(":avalon-modules:avalon-dataset:avalon-dataset-controller"))
    implementation("org.babyfish.jimmer:jimmer-spring-boot-starter")
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-web")
    runtimeOnly("org.postgresql:postgresql")
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
