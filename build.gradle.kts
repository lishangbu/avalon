import io.github.lishangbu.avalon.build.support.configurePublication
import io.github.lishangbu.avalon.build.support.defaultCoverageExclusions
import io.github.lishangbu.avalon.build.support.dockerImageNameProvider
import io.github.lishangbu.avalon.build.support.mainClassDirectories
import io.github.lishangbu.avalon.build.support.mainSourceDirectories
import io.github.lishangbu.avalon.build.tasks.AssembleReportSiteTask
import io.github.lishangbu.avalon.build.tasks.DownloadIpDataTask
import io.github.lishangbu.avalon.build.tasks.GenerateRsaKeysTask
import io.github.lishangbu.avalon.build.tasks.PrintValueTask
import org.gradle.api.tasks.testing.TestReport
import org.gradle.jvm.tasks.Jar
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.springframework.boot.gradle.tasks.bundling.BootBuildImage
import java.math.BigDecimal

// 根工程统一注册插件，子模块只在真正需要时再接入对应约定
plugins {
    base
    jacoco
    alias(libs.plugins.ktlint)
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.kotlin.jvm) apply false
    alias(libs.plugins.kotlin.spring) apply false
    alias(libs.plugins.kotlin.kapt) apply false
    alias(libs.plugins.spring.boot) apply false
    alias(libs.plugins.dokka)
}

// 共享构建约定按插件类型延迟挂载，避免无关模块承担额外配置成本
subprojects {
    // Java 模块共享工具链、测试、发布和签名的默认配置
    pluginManager.withPlugin("java") {
        apply(plugin = "jacoco")

        extensions.configure<JavaPluginExtension> {
            toolchain.languageVersion.set(JavaLanguageVersion.of(25))
            withSourcesJar()
            withJavadocJar()
        }

        extensions.configure<JacocoPluginExtension> {
            toolVersion = "0.8.14"
        }

        configurations.named("testCompileOnly") {
            extendsFrom(configurations.getByName("compileOnly"))
        }

        dependencies {
            add("testImplementation", libs.kotlin.test.junit5)
            add("testImplementation", libs.spring.boot.starter.test)
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
            jvmArgs("--enable-native-access=ALL-UNNAMED")
            doFirst {
                // 较新的 JDK 上，Mockito inline mock 需要显式附加 javaagent
                val mockitoCore = classpath.files.firstOrNull { it.name.startsWith("mockito-core-") }
                if (mockitoCore != null) {
                    jvmArgs("-javaagent:${mockitoCore.absolutePath}")
                }
            }
        }

        configurePublication(publicationName = "mavenJava", componentName = "java")
    }

    // 类库模块统一导入 BOM，叶子模块就不需要重复声明依赖版本
    pluginManager.withPlugin("java-library") {
        dependencies {
            add("implementation", platform(libs.spring.boot.bom))
            add("implementation", platform(libs.aws.bom))
            add("implementation", platform(libs.jimmer.bom))
        }
    }

    // Boot 应用继承统一 BOM，并默认关闭镜像推送
    pluginManager.withPlugin("org.springframework.boot") {
        tasks.withType<BootBuildImage>().configureEach {
            imageName.set(project.dockerImageNameProvider())
            publish.set(false)
        }

        dependencies {
            add("implementation", platform(libs.spring.boot.bom))
            add("implementation", libs.spring.boot.starter.liquibase)
            add("implementation", platform(libs.aws.bom))
            add("implementation", platform(libs.jimmer.bom))
        }
    }

    // 使用 KSP 的模块需要和运行时依赖保持同一套 Jimmer 版本
    pluginManager.withPlugin("com.google.devtools.ksp") {
        dependencies {
            add("ksp", platform(libs.jimmer.bom))
        }
    }

    // Kotlin/JVM 模块共享 lint、编译级别和反射依赖等默认约定
    pluginManager.withPlugin("org.jetbrains.kotlin.jvm") {
        apply(plugin = "org.jlleitschuh.gradle.ktlint")
        extensions.configure<KtlintExtension> {
            filter {
                exclude("**/package-info.kt")
                exclude { treeElement ->
                    val normalizedPath = treeElement.file.path.replace('\\', '/')
                    normalizedPath.contains("/build/generated/")
                }
            }
        }

        dependencies {
            add("implementation", libs.kotlin.reflect)
        }

        tasks.withType<KotlinCompile>().configureEach {
            compilerOptions {
                jvmTarget.set(JvmTarget.JVM_25)
                languageVersion.set(KotlinVersion.KOTLIN_2_3)
                apiVersion.set(KotlinVersion.KOTLIN_2_3)
                javaParameters.set(true)
                freeCompilerArgs.add("-Xjsr305=strict")
            }
        }
    }

    // Dokka 产物打进 javadocJar，满足 Maven Central 对发布物的常见要求
    pluginManager.withPlugin("org.jetbrains.dokka") {
        rootProject.dependencies.add("dokka", project(path))

        tasks.named<Jar>("javadocJar").configure {
            dependsOn(tasks.named("dokkaGeneratePublicationHtml"))
            from(layout.buildDirectory.dir("dokka/html"))
        }
    }

    // BOM 模块沿用和普通发布模块一致的仓库与签名规则
    pluginManager.withPlugin("java-platform") {
        configurePublication(publicationName = "mavenBom", componentName = "javaPlatform")
    }
}

val coverageProjects =
    subprojects.filter { project ->
        project.layout.projectDirectory
            .dir("src/main/kotlin")
            .asFile
            .exists() ||
            project.layout.projectDirectory
                .dir("src/main/java")
                .asFile
                .exists()
    }

val aggregateTestReportDirectory = layout.buildDirectory.dir("reports/tests/aggregateTestReport")
val aggregateCoverageHtmlDirectory = layout.buildDirectory.dir("reports/jacoco/aggregateCoverageReport/html")
val aggregateCoverageXmlFile = layout.buildDirectory.file("reports/jacoco/aggregateCoverageReport/aggregateCoverageReport.xml")
val aggregateReportsSiteDirectory = layout.buildDirectory.dir("reports/github-pages/quality")

val aggregateTestReport by tasks.registering(TestReport::class) {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Generates an aggregate test report for all JVM modules."

    destinationDirectory.set(aggregateTestReportDirectory)
    testResults.from(coverageProjects.map { it.layout.buildDirectory.dir("test-results/test/binary") })
    dependsOn(coverageProjects.map { it.tasks.named("test") })
}

val aggregateCoverageReport by tasks.registering(JacocoReport::class) {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Generates an aggregate JaCoCo coverage report for all JVM modules."

    dependsOn(coverageProjects.map { it.tasks.named("test") })
    executionData.from(coverageProjects.map { it.layout.buildDirectory.file("jacoco/test.exec") })
    additionalSourceDirs.from(coverageProjects.flatMap { it.mainSourceDirectories() })
    sourceDirectories.from(coverageProjects.flatMap { it.mainSourceDirectories() })
    classDirectories.from(coverageProjects.flatMap { it.mainClassDirectories(defaultCoverageExclusions) })

    reports {
        xml.required.set(true)
        xml.outputLocation.set(aggregateCoverageXmlFile)
        html.required.set(true)
        html.outputLocation.set(aggregateCoverageHtmlDirectory)
        csv.required.set(false)
    }
}

val aggregateCoverageVerification by tasks.registering(JacocoCoverageVerification::class) {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Verifies aggregate line coverage stays at or above 90%."

    dependsOn(aggregateCoverageReport)
    executionData.from(coverageProjects.map { it.layout.buildDirectory.file("jacoco/test.exec") })
    additionalSourceDirs.from(coverageProjects.flatMap { it.mainSourceDirectories() })
    sourceDirectories.from(coverageProjects.flatMap { it.mainSourceDirectories() })
    classDirectories.from(coverageProjects.flatMap { it.mainClassDirectories(defaultCoverageExclusions) })

    violationRules {
        rule {
            limit {
                counter = "LINE"
                value = "COVEREDRATIO"
                minimum = BigDecimal("0.90")
            }
        }
    }
}

val aggregateReportsSite by tasks.registering(AssembleReportSiteTask::class) {
    group = LifecycleBasePlugin.VERIFICATION_GROUP
    description = "Packages aggregate test and coverage reports into a static site for GitHub Pages."

    dependsOn(aggregateTestReport, aggregateCoverageReport)
    testReportDirectory.set(aggregateTestReportDirectory)
    coverageReportDirectory.set(aggregateCoverageHtmlDirectory)
    coverageXmlReport.set(aggregateCoverageXmlFile)
    outputDirectory.set(aggregateReportsSiteDirectory)
}

tasks.named(LifecycleBasePlugin.CHECK_TASK_NAME) {
    dependsOn(aggregateCoverageVerification)
}

// 根任务主要服务于 CI 工作流和本地初始化命令
tasks.register<PrintValueTask>("printVersion") {
    group = "build setup"
    description = "Print the version of this project."
    value.set(providers.gradleProperty("version"))
}

val defaultIpDbFiles = listOf("IP2LOCATION-LITE-DB11.IPV6.BIN")

tasks.register<DownloadIpDataTask>("downloadIpData") {
    group = "build setup"
    description = "Downloads and normalizes IP2Location database files into module resources."

    // 允许外部覆盖下载参数，但日常构建仍然提供可直接使用的默认值
    val configuredDbVersion =
        providers
            .gradleProperty("ipDbVersion")
            .orNull
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?: "2025.12.01"
    val configuredDbFiles =
        providers
            .gradleProperty("ipDbFiles")
            .orNull
            ?.split(',')
            ?.map { it.trim() }
            ?.filter { it.isNotBlank() }
            ?.distinct()
            ?.takeIf { it.isNotEmpty() }
            ?: defaultIpDbFiles

    dbVersion.set(configuredDbVersion)
    dbFileNames.set(configuredDbFiles)
    destinationDir.set(
        layout.projectDirectory.dir(
            "avalon-extensions/avalon-ip2location-spring-boot-starter/src/main/resources",
        ),
    )
    repoRootDir.set(layout.projectDirectory)
}

tasks.register<GenerateRsaKeysTask>("generateRsaKeys") {
    group = "build setup"
    description = "Generates one RSA key pair and writes it to configured application resource directories."

    // CI 可以显式覆盖已有密钥；本地执行默认保持保守策略，避免误覆盖
    val configuredKeySize =
        providers
            .gradleProperty("keySize")
            .orNull
            ?.trim()
            ?.takeIf { it.isNotBlank() }
            ?.toIntOrNull()
            ?: 2048
    val overwriteExistingKeys =
        when (
            providers
                .gradleProperty("force")
                .orNull
                ?.trim()
                ?.lowercase()
        ) {
            "true", "1", "yes", "y", "on" -> true
            else -> false
        }
    val rsaApplicationPaths =
        listOf(
            "avalon-application/avalon-admin-server",
            "avalon-application/avalon-standalone-server",
        )

    keySize.set(configuredKeySize)
    forceOverwrite.set(overwriteExistingKeys)
    applicationPaths.set(rsaApplicationPaths)
    outputFiles.from(
        rsaApplicationPaths.flatMap { appPath ->
            listOf(
                layout.projectDirectory.file("$appPath/src/main/resources/rsa/private_key.pem"),
                layout.projectDirectory.file("$appPath/src/main/resources/rsa/public_key.pem"),
            )
        },
    )
    repoRootDir.set(layout.projectDirectory)
}
