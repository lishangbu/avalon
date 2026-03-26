plugins {
    `kotlin-dsl`
}

repositories {
    // 默认开发环境优先走镜像仓库，减少国内拉取依赖的等待时间。
    maven("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/")
    // Gradle 插件依赖仍然优先支持从官方插件门户解析。
    gradlePluginPortal()
    // buildSrc 的普通库依赖最终回落到 Maven Central。
    mavenCentral()
}
