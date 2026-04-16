import org.gradle.api.artifacts.VersionCatalogsExtension
import org.gradle.kotlin.dsl.getByType

plugins {
    `kotlin-dsl`
}

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")
val kotlinVersion = libs.findVersion("kotlin").get().requiredVersion

dependencies {
    // 这里需要运行时可见，根工程执行 buildSrc 辅助函数时才拿得到 Kotlin Gradle API 类型。
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:$kotlinVersion")
    implementation("org.jetbrains.kotlin:kotlin-allopen:$kotlinVersion")
}
