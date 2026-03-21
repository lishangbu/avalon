plugins {
    `java-library`
    id("com.google.devtools.ksp")
    id("org.jetbrains.kotlin.jvm")
    id("org.jetbrains.kotlin.plugin.spring")
    id("org.jetbrains.dokka")
}

dependencies {
    api(project(":avalon-extensions:avalon-jimmer-extension"))
    api("org.babyfish.jimmer:jimmer-sql-kotlin")
    api("com.fasterxml.jackson.core:jackson-databind")
    ksp("org.babyfish.jimmer:jimmer-ksp")
}

kotlin {
    // 让 IDE 与编译都能识别 KSP 生成代码
    sourceSets.main {
        kotlin.srcDir("build/generated/ksp/main/kotlin")
    }
}
