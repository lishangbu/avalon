plugins {
    `java-library`
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.kotlin.kapt)
    alias(libs.plugins.dokka)
}

dependencies {
    api(libs.ip2location)
    compileOnly(libs.slf4j.api)
    api(libs.spring.boot.autoconfigure)
    kapt(libs.spring.boot.configuration.processor)
}

tasks.test {
    exclude("**/IpToLocationSearcherTest*")
}

tasks.processResources {
    mustRunAfter(rootProject.tasks.named("downloadIpData"))
}

tasks.named<Jar>("sourcesJar") {
    mustRunAfter(rootProject.tasks.named("downloadIpData"))
}
