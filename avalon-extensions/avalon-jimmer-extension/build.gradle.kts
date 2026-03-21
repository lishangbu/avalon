plugins {
    `java-library`
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.spring")
    id("org.jetbrains.dokka")
}

dependencies {
    implementation("org.babyfish.jimmer:jimmer-core")
    implementation("org.springframework.boot:spring-boot-jackson")
    implementation("org.springframework.boot:spring-boot-autoconfigure")
}
