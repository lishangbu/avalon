package io.github.lishangbu.avalon.gradle.support

import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.allopen.gradle.AllOpenExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

/**
 * 把 Avalon 多模块工程共享的 JVM 编译与测试约定集中收口到 buildSrc。
 *
 * 这里保持当前仓库已经验证过的 Java 25、`-Xjsr305=strict`、AllOpen 注解和
 * JUnit Platform 行为，只抽离公共任务配置，不改变模块边界或依赖关系。
 */
fun Project.configureAvalonJvmConventions() {
    pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
        extensions.configure<JavaPluginExtension> {
            sourceCompatibility = JavaVersion.VERSION_25
            targetCompatibility = JavaVersion.VERSION_25
            withSourcesJar()
            withJavadocJar()
        }

        tasks.withType<KotlinCompile>().configureEach {
            compilerOptions {
                jvmTarget.set(JvmTarget.fromTarget("25"))
                freeCompilerArgs.add("-Xjsr305=strict")
            }
        }
    }

    pluginManager.withPlugin("org.jetbrains.kotlin.plugin.allopen") {
        extensions.configure<AllOpenExtension> {
            annotation("jakarta.ws.rs.Path")
            annotation("jakarta.enterprise.context.ApplicationScoped")
            annotation("io.quarkus.test.junit.QuarkusTest")
        }
    }

    pluginManager.withPlugin("java") {
        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
        }
    }

    pluginManager.withPlugin("java-library") {
        configurePublication(publicationName = "mavenJava", componentName = "java")
    }
}
