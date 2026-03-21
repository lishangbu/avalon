plugins {
    `java-library`
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.spring")
    id("org.jetbrains.dokka")
}

dependencies {
    api(project(":avalon-extensions:avalon-jimmer-extension"))
    api(project(":avalon-support:avalon-oauth2-support:avalon-oauth2-authorization-server"))
    api("org.babyfish.jimmer:jimmer-spring-boot-starter")
    api("org.babyfish.jimmer:jimmer-sql-kotlin")
    api("org.springframework.boot:spring-boot-starter-jdbc")
    api("org.springframework.boot:spring-boot-starter-data-redis")
    api("org.springframework.boot:spring-boot-starter-validation")
    ksp("org.babyfish.jimmer:jimmer-ksp")
    testImplementation("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-liquibase")
    testImplementation("org.springframework.boot:spring-boot-testcontainers")
    testImplementation("org.springframework.security:spring-security-test")
    testImplementation("org.testcontainers:testcontainers-junit-jupiter")
    testImplementation("org.testcontainers:testcontainers-postgresql")
}

kotlin {
    // 让 IDE 与编译都能识别 KSP 生成代码
    sourceSets.main {
        kotlin.srcDir("build/generated/ksp/main/kotlin")
    }
}

tasks.processTestResources {
    // 测试时复制根目录 db，供 Liquibase 加载
    from(rootProject.layout.projectDirectory.dir("db")) {
        into("db")
    }
}
