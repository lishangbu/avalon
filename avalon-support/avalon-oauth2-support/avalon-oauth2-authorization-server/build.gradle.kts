plugins {
    `java-library`
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.spring")
    id("org.jetbrains.dokka")
}

dependencies {
    api(project(":avalon-support:avalon-oauth2-support:avalon-oauth2-resource-server"))
    api("org.springframework.boot:spring-boot-autoconfigure")
    api("org.springframework.boot:spring-boot-starter-oauth2-authorization-server")
    api("org.springframework.boot:spring-boot-starter-oauth2-client")
}
