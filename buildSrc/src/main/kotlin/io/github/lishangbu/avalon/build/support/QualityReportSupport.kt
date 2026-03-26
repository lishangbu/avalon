package io.github.lishangbu.avalon.build.support

import org.gradle.api.Project
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.testing.TestReport
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport
import java.math.BigDecimal

/**
 * 找出真正包含 JVM 主源码的子模块。
 *
 * 聚合测试报告和聚合 JaCoCo 报告都只针对这类模块，
 * 防止空模块把无意义的路径带进任务输入。
 */
fun Project.projectsWithMainSources(): List<Project> =
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

/**
 * 注册聚合测试报告任务。
 */
fun Project.registerAggregateTestReportTask(
    coverageProjects: List<Project>,
    destinationDirectory: Provider<Directory>,
): TaskProvider<TestReport> =
    tasks.register("aggregateTestReport", TestReport::class.java) {
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        description = "Generates an aggregate test report for all JVM modules."

        this.destinationDirectory.set(destinationDirectory)
        testResults.from(coverageProjects.map { it.layout.buildDirectory.dir("test-results/test/binary") })
        dependsOn(coverageProjects.map { it.tasks.named("test") })
    }

/**
 * 注册聚合 JaCoCo 报告任务。
 */
fun Project.registerAggregateCoverageReportTask(
    coverageProjects: List<Project>,
    htmlOutputDirectory: Provider<Directory>,
    xmlOutputFile: Provider<RegularFile>,
    coverageExclusions: List<String> = defaultCoverageExclusions,
): TaskProvider<JacocoReport> =
    tasks.register("aggregateCoverageReport", JacocoReport::class.java) {
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        description = "Generates an aggregate JaCoCo coverage report for all JVM modules."

        dependsOn(coverageProjects.map { it.tasks.named("test") })
        executionData.from(coverageProjects.map { it.layout.buildDirectory.file("jacoco/test.exec") })
        additionalSourceDirs.from(coverageProjects.flatMap { it.mainSourceDirectories() })
        sourceDirectories.from(coverageProjects.flatMap { it.mainSourceDirectories() })
        classDirectories.from(coverageProjects.flatMap { it.mainClassDirectories(coverageExclusions) })

        reports {
            xml.required.set(true)
            xml.outputLocation.set(xmlOutputFile)
            html.required.set(true)
            html.outputLocation.set(htmlOutputDirectory)
            csv.required.set(false)
        }
    }

/**
 * 注册聚合覆盖率校验任务。
 */
fun Project.registerAggregateCoverageVerificationTask(
    coverageProjects: List<Project>,
    reportTask: TaskProvider<JacocoReport>,
    coverageExclusions: List<String> = defaultCoverageExclusions,
    minimumLineCoverage: BigDecimal = BigDecimal("0.90"),
): TaskProvider<JacocoCoverageVerification> =
    tasks.register("aggregateCoverageVerification", JacocoCoverageVerification::class.java) {
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        description = "Verifies aggregate line coverage stays at or above 90%."

        dependsOn(reportTask)
        executionData.from(coverageProjects.map { it.layout.buildDirectory.file("jacoco/test.exec") })
        additionalSourceDirs.from(coverageProjects.flatMap { it.mainSourceDirectories() })
        sourceDirectories.from(coverageProjects.flatMap { it.mainSourceDirectories() })
        classDirectories.from(coverageProjects.flatMap { it.mainClassDirectories(coverageExclusions) })

        violationRules {
            rule {
                limit {
                    counter = "LINE"
                    value = "COVEREDRATIO"
                    minimum = minimumLineCoverage
                }
            }
        }
    }
