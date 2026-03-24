plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.dokka)
}

dependencies {
    api(project(":avalon-modules:avalon-dataset:avalon-dataset-model"))
    api(libs.jimmer.spring.boot.starter)
    api(libs.spring.data.commons)
    testImplementation(libs.spring.boot.starter.liquibase)
    testImplementation(libs.postgresql)
    testImplementation(libs.spring.boot.testcontainers)
    testImplementation(libs.spring.security.test)
    testImplementation(libs.testcontainers.junit.jupiter)
    testImplementation(libs.testcontainers.postgresql)
}

tasks.processTestResources {
    // Make root db/changelog available for Liquibase in repository integration tests.
    from(rootProject.layout.projectDirectory.dir("db")) {
        into("db")
    }
}
