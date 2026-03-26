package io.github.lishangbu.avalon.build.support

import org.gradle.api.Project
import org.gradle.api.artifacts.dsl.DependencyHandler
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.jvm.tasks.Jar
import org.gradle.jvm.toolchain.JavaLanguageVersion
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.named
import org.gradle.kotlin.dsl.withType
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension

private typealias DependencyConfiguration = DependencyHandler.() -> Unit

/**
 * 为所有应用了 `java` 插件的模块挂载统一的 JVM/测试/发布约定。
 *
 * 根脚本只需要提供测试依赖，具体的 Java toolchain、JaCoCo 和 `Test`
 * 任务细节都集中放到 buildSrc，减少 IDE 在根脚本上解析大量 Gradle API 类型。
 */
fun Project.configureJavaModuleConventions(
    configureTestDependencies: DependencyConfiguration = {},
) {
    pluginManager.withPlugin("java") {
        pluginManager.apply("jacoco")

        extensions.configure<JavaPluginExtension> {
            toolchain.languageVersion.set(JavaLanguageVersion.of(25))
            withSourcesJar()
            withJavadocJar()
        }

        extensions.configure<JacocoPluginExtension> {
            toolVersion = "0.8.14"
        }

        configurations.named("testCompileOnly") {
            extendsFrom(this@configureJavaModuleConventions.configurations.getByName("compileOnly"))
        }

        dependencies {
            configureTestDependencies()
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
            jvmArgs("--enable-native-access=ALL-UNNAMED")
            doFirst {
                // 较新的 JDK 上，Mockito inline mock 需要显式附加 javaagent。
                val mockitoCore = classpath.files.firstOrNull { it.name.startsWith("mockito-core-") }
                if (mockitoCore != null) {
                    jvmArgs("-javaagent:${mockitoCore.absolutePath}")
                }
            }
        }

        configurePublication(publicationName = "mavenJava", componentName = "java")
    }
}

/**
 * 为 `java-library` 模块集中补齐各类 BOM 依赖。
 *
 * 这样叶子模块只声明自己真正关心的 starter/库坐标，不需要重复写平台依赖。
 */
fun Project.configureJavaLibraryBomConventions(
    configureImplementationDependencies: DependencyConfiguration = {},
) {
    pluginManager.withPlugin("java-library") {
        dependencies {
            configureImplementationDependencies()
        }
    }
}

/**
 * 把 Dokka 产物接入 `javadocJar`，满足中央仓库对 javadoc 制品的常见要求。
 */
fun Project.configureDokkaJavadocJarIntegration() {
    pluginManager.withPlugin("org.jetbrains.dokka") {
        rootProject.dependencies.add("dokka", project(path))

        tasks.named<Jar>("javadocJar").configure {
            dependsOn(tasks.named("dokkaGeneratePublicationHtml"))
            from(layout.buildDirectory.dir("dokka/html"))
        }
    }
}

/**
 * 为 `java-platform` 模块沿用统一的发布和签名约定。
 */
fun Project.configureJavaPlatformPublicationConvention() {
    pluginManager.withPlugin("java-platform") {
        configurePublication(publicationName = "mavenBom", componentName = "javaPlatform")
    }
}
