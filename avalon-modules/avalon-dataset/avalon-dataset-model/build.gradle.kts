plugins {
    `java-library`
    alias(libs.plugins.ksp)
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.spring)
    alias(libs.plugins.dokka)
}

dependencies {
    api(project(":avalon-extensions:avalon-jimmer-extension"))
    api(libs.jimmer.sql.kotlin)
    api(libs.jackson.databind)
    ksp(libs.jimmer.ksp)
}

kotlin {
    // 让 IDE 与编译都能识别 KSP 生成代码
    sourceSets.main {
        kotlin.srcDir("build/generated/ksp/main/kotlin")
    }
}
