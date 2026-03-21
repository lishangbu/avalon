plugins {
    `java-library`
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.spring")
    id("org.jetbrains.dokka")
}

dependencies {
    api("net.renfei:ip2location:1.2.6")
    compileOnly("org.slf4j:slf4j-api")
    api("org.springframework.boot:spring-boot-autoconfigure")
    compileOnly("org.springframework.boot:spring-boot-configuration-processor")
    annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")
}

tasks.test {
    exclude("**/IpToLocationSearcherTest*")
}
