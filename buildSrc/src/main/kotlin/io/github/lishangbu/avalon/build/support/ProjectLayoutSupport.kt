package io.github.lishangbu.avalon.build.support

import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.FileTree
import org.gradle.api.provider.Provider

/**
 * 各模块聚合 JaCoCo 报告时使用的默认排除规则。
 *
 * 这里会跳过启动类和生成代码，避免这些并不代表核心业务逻辑的产物
 * 稀释覆盖率基线，导致聚合报告失真。
 */
val defaultCoverageExclusions =
    listOf(
        "**/*Application*",
        "**/*Draft*",
        "**/*\$DefaultImpls.class",
        "**/package-info.*",
    )

/**
 * 计算 Spring Boot OCI 镜像构建时的默认镜像名。
 *
 * 返回值保持为惰性 Provider，这样 Gradle 仍能配合 configuration cache，
 * 同时支持在执行阶段通过属性覆盖仓库或前缀。
 */
fun Project.dockerImageNameProvider(): Provider<String> {
    val imageName = name.lowercase()

    return providers
        .gradleProperty("dockerRepository")
        .zip(providers.gradleProperty("dockerImagePrefix")) { repository, prefix ->
            "$repository/$prefix/$imageName:latest"
        }
}

/**
 * 仅返回当前模块里实际存在的源码目录。
 *
 * 聚合报告任务通过这个辅助函数避开空目录，防止某些只有 Java 或只有 Kotlin
 * 的模块注册出无效路径。
 */
fun Project.mainSourceDirectories(): List<Directory> =
    listOf(
        layout.projectDirectory.dir("src/main/kotlin"),
        layout.projectDirectory.dir("src/main/java"),
    ).filter { it.asFile.exists() }

/**
 * 为聚合覆盖率报告构造过滤后的 class 目录。
 *
 * JaCoCo 聚合时需要排除生成类和应用入口类，否则这类不适合纳入考核的代码
 * 会直接影响最终覆盖率统计结果。
 */
fun Project.mainClassDirectories(coverageExclusions: List<String> = defaultCoverageExclusions): List<FileTree> =
    listOf(
        layout.buildDirectory.dir("classes/kotlin/main"),
        layout.buildDirectory.dir("classes/java/main"),
    ).map { classesDir ->
        fileTree(classesDir) {
            exclude(coverageExclusions)
        }
    }
