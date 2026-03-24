plugins {
    `java-library`
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.dokka)
}

dependencies {
    api(project(":avalon-extensions:avalon-jimmer-extension"))
    api(project(":avalon-support:avalon-oauth2-support:avalon-oauth2-authorization-server"))
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
