plugins {
    `java-library`
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.spring")
    id("org.jetbrains.dokka")
}

dependencies {
    api(project(":avalon-modules:avalon-dataset:avalon-dataset-model"))
    api("org.babyfish.jimmer:jimmer-spring-boot-starter")
    api("org.springframework.data:spring-data-commons")
    testImplementation("org.springframework.boot:spring-boot-starter-liquibase")
    testImplementation("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")
}

tasks.processTestResources {
    // Make root db/changelog available for Liquibase in repository integration tests.
    from(rootProject.layout.projectDirectory.dir("db")) {
        into("db")
    }
}
