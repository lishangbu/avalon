plugins {
    alias(libs.plugins.quarkus)
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.allopen")
}

dependencies {
    implementation(enforcedPlatform(libs.quarkus.bom))

    implementation(project(":modules:shared-kernel"))
    implementation(project(":modules:shared-application"))
    implementation(project(":modules:shared-infra"))
    implementation(project(":modules:identity-access"))
    implementation(project(":modules:catalog"))
    implementation(project(":modules:player"))
    implementation(project(":modules:battle"))

    implementation(libs.quarkus.arc)
    implementation(libs.quarkus.config.yaml)
    implementation(libs.quarkus.kotlin)
    implementation(libs.quarkus.rest)
    implementation(libs.quarkus.rest.jackson)
    implementation(libs.quarkus.hibernate.validator)
    implementation(libs.quarkus.scheduler)
    implementation(libs.quarkus.smallrye.openapi)
    implementation(libs.quarkus.smallrye.health)
    implementation(libs.quarkus.reactive.pg.client)
    implementation(libs.quarkus.jdbc.postgresql)
    implementation(libs.quarkus.flyway)
    implementation(libs.flyway.database.postgresql)
    implementation(libs.jackson.module.kotlin)
    implementation(libs.quarkus.redis.client)
    implementation(libs.quarkus.websockets.next)

    testImplementation(libs.quarkus.junit5)
    testImplementation(libs.quarkus.elytron.security.common)
    testImplementation(libs.rest.assured)
}
