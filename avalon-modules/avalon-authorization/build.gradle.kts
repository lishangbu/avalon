import org.gradle.api.tasks.PathSensitivity

plugins {
    `java-library`
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.dokka)
}

val mainDtoDir = layout.projectDirectory.dir("src/main/dto")

dependencies {
    api(project(":avalon-platform:avalon-jimmer"))
    api(project(":avalon-platform:avalon-security:avalon-oauth2-authorization-server"))
    api(libs.jimmer.spring.boot.starter)
    api(libs.jimmer.sql.kotlin)
    api(libs.spring.boot.starter.jdbc)
    api(libs.spring.boot.starter.data.redis)
    api(libs.spring.boot.starter.validation)
    ksp(libs.jimmer.ksp)
    testImplementation(libs.postgresql)
    testImplementation(libs.spring.boot.starter.liquibase)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.spring.security.test)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)
}

tasks.configureEach {
    if (name == "kspKotlin") {
        // Make DTO definitions part of the cache key for clean and remote builds.
        inputs
            .dir(mainDtoDir)
            .withPropertyName("jimmerMainDtoDir")
            .withPathSensitivity(PathSensitivity.RELATIVE)
    }
}

tasks.processTestResources {
    // 测试时复制根目录 db，供 Liquibase 加载
    from(rootProject.layout.projectDirectory.dir("db")) {
        into("db")
    }
}
