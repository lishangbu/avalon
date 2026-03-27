import io.github.lishangbu.avalon.build.support.configureDokkaJavadocJarIntegration
import io.github.lishangbu.avalon.build.support.configureJavaLibraryBomConventions
import io.github.lishangbu.avalon.build.support.configureJavaModuleConventions
import io.github.lishangbu.avalon.build.support.configureJavaPlatformPublicationConvention
import io.github.lishangbu.avalon.build.support.dockerImageNameProvider
import io.github.lishangbu.avalon.build.support.projectsWithMainSources
import io.github.lishangbu.avalon.build.support.registerAggregateCoverageReportTask
import io.github.lishangbu.avalon.build.support.registerAggregateCoverageVerificationTask
import io.github.lishangbu.avalon.build.support.registerAggregateTestReportTask
import io.github.lishangbu.avalon.build.tasks.AssembleReportSiteTask
import io.github.lishangbu.avalon.build.tasks.DownloadIpDataTask
import io.github.lishangbu.avalon.build.tasks.GenerateRsaKeysTask
import io.github.lishangbu.avalon.build.tasks.PrintValueTask
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.springframework.boot.gradle.tasks.bundling.BootBuildImage

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
    configureJavaModuleConventions {
        add("testImplementation", libs.kotlin.test.junit5)
        add("testImplementation", libs.spring.boot.starter.test)
    }

    // 类库模块统一导入 BOM，叶子模块就不需要重复声明依赖版本
    configureJavaLibraryBomConventions {
        add("implementation", platform(libs.spring.boot.bom))
        add("implementation", platform(libs.aws.bom))
        add("implementation", platform(libs.jimmer.bom))
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
            version.set("1.8.0")
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
    configureDokkaJavadocJarIntegration()

    // BOM 模块沿用和普通发布模块一致的仓库与签名规则
    configureJavaPlatformPublicationConvention()
}

val coverageProjects = projectsWithMainSources()

val aggregateTestReportDirectory = layout.buildDirectory.dir("reports/tests/aggregateTestReport")
val aggregateCoverageHtmlDirectory = layout.buildDirectory.dir("reports/jacoco/aggregateCoverageReport/html")
val aggregateCoverageXmlFile = layout.buildDirectory.file("reports/jacoco/aggregateCoverageReport/aggregateCoverageReport.xml")
val aggregateReportsSiteDirectory = layout.buildDirectory.dir("reports/github-pages/quality")

val aggregateTestReport = registerAggregateTestReportTask(coverageProjects, aggregateTestReportDirectory)

val aggregateCoverageReport =
    registerAggregateCoverageReportTask(
        coverageProjects = coverageProjects,
        htmlOutputDirectory = aggregateCoverageHtmlDirectory,
        xmlOutputFile = aggregateCoverageXmlFile,
    )

val aggregateCoverageVerification =
    registerAggregateCoverageVerificationTask(
        coverageProjects = coverageProjects,
        reportTask = aggregateCoverageReport,
    )

val aggregateReportsSite by tasks.registering(AssembleReportSiteTask::class) {
    group = "verification"
    description = "Packages aggregate test and coverage reports into a static site for GitHub Pages."

    dependsOn(aggregateTestReport, aggregateCoverageReport)
    testReportDirectory.set(aggregateTestReportDirectory)
    coverageReportDirectory.set(aggregateCoverageHtmlDirectory)
    coverageXmlReport.set(aggregateCoverageXmlFile)
    outputDirectory.set(aggregateReportsSiteDirectory)
}

tasks.named("check") {
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
    description = "Normalizes local IP2Location database files and downloads from the official service when needed."

    val propertyDownloadToken =
        providers
            .gradleProperty("ipDbDownloadToken")
            .orNull
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    val environmentDownloadToken =
        providers
            .environmentVariable("IP2LOCATION_DOWNLOAD_TOKEN")
            .orNull
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    val configuredDownloadToken = propertyDownloadToken ?: environmentDownloadToken
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
    val shouldForceRefresh =
        when (
            providers
                .gradleProperty("refreshIpDb")
                .orNull
                ?.trim()
                ?.lowercase()
        ) {
            "true", "1", "yes", "y", "on" -> true
            else -> false
        }

    dbFileNames.set(configuredDbFiles)
    forceRefresh.set(shouldForceRefresh)
    if (configuredDownloadToken != null) {
        downloadToken.set(configuredDownloadToken)
    }
    destinationDir.set(
        layout.projectDirectory.dir(
            "avalon-platform/avalon-ip2location-spring-boot-starter/src/main/resources",
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
