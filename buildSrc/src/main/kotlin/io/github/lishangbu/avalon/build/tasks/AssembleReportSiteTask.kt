package io.github.lishangbu.avalon.build.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import javax.inject.Inject

/**
 * 把聚合测试报告和覆盖率报告整理成静态站点目录结构。
 *
 * 输出目录刻意保持对 GitHub Pages 友好，CI 可以直接发布，
 * 不需要再额外做一次站点生成。
 */
@DisableCachingByDefault(because = "Bundles generated reports into a static site directory.")
abstract class AssembleReportSiteTask : DefaultTask() {
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val testReportDirectory: DirectoryProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val coverageReportDirectory: DirectoryProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val coverageXmlReport: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDirectory: DirectoryProperty

    @get:Inject
    abstract val fileSystemOperations: FileSystemOperations

    @TaskAction
    fun assemble() {
        val siteDirectory = outputDirectory.get().asFile
        if (siteDirectory.exists()) {
            siteDirectory.deleteRecursively()
        }
        siteDirectory.mkdirs()

        fileSystemOperations.copy {
            from(testReportDirectory)
            into(siteDirectory.resolve("test-reports"))
        }
        fileSystemOperations.copy {
            from(coverageReportDirectory)
            into(siteDirectory.resolve("coverage"))
        }
        fileSystemOperations.copy {
            from(coverageXmlReport)
            into(siteDirectory.resolve("coverage"))
        }

        // 禁用 Jekyll 处理，确保报告资源按生成结果原样对外提供。
        siteDirectory.resolve(".nojekyll").writeText("")
    }
}
