package io.github.lishangbu.avalon.build.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption

private fun File.normalizedPath(): String = path.replace(File.separatorChar, '/')

/**
 * 下载并归并 IP2Location 数据文件到统一的资源目录。
 *
 * 任务会优先复用仓库里已经存在的最新文件，只有在找不到可复用副本时
 * 才重新下载，这样本地开发和 CI 恢复缓存的成本都更低。
 */
@DisableCachingByDefault(because = "Downloads external IP data into project resources.")
abstract class DownloadIpDataTask : DefaultTask() {
    @get:Input
    abstract val dbVersion: Property<String>

    @get:Input
    abstract val dbFileNames: ListProperty<String>

    @get:OutputDirectory
    abstract val destinationDir: DirectoryProperty

    @get:Internal
    abstract val repoRootDir: DirectoryProperty

    @TaskAction
    fun download() {
        val filesToProcess =
            dbFileNames
                .get()
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()
        if (filesToProcess.isEmpty()) {
            throw GradleException("No IP database files configured. Set -PipDbFiles=file1,file2.")
        }

        val destinationRoot = destinationDir.get().asFile
        val repoRoot = repoRootDir.get().asFile
        destinationRoot.mkdirs()

        val urlPrefix = "https://github.com/renfei/ip2location/releases/download/${dbVersion.get()}"

        filesToProcess.forEach { dbFileName ->
            val destinationFile = destinationRoot.resolve(dbFileName)
            val destinationCanonical = destinationFile.canonicalFile

            // 先在仓库内查找同名文件，尽量复用已有副本，
            // 避免每台机器或每次 CI 都重复下载。
            val candidates =
                repoRoot
                    .walkTopDown()
                    .onEnter { directory -> directory.name !in setOf(".git", ".gradle", "build") }
                    .filter { candidate -> candidate.isFile && candidate.name == dbFileName }
                    .map { candidate -> candidate.canonicalFile }
                    .distinctBy { candidate -> candidate.normalizedPath() }
                    .toMutableList()

            if (destinationCanonical.exists() && destinationCanonical !in candidates) {
                candidates.add(destinationCanonical)
            }

            if (candidates.isEmpty()) {
                val downloadUrl = "$urlPrefix/$dbFileName"
                logger.lifecycle("Downloading: $downloadUrl")
                URI(downloadUrl).toURL().openStream().use { inputStream ->
                    Files.copy(
                        inputStream,
                        destinationCanonical.toPath(),
                        StandardCopyOption.REPLACE_EXISTING,
                    )
                }
                return@forEach
            }

            val newestFile = candidates.maxByOrNull { candidate -> candidate.lastModified() } ?: destinationCanonical
            logger.lifecycle("Keeping newest file: ${newestFile.normalizedPath()}")
            if (newestFile != destinationCanonical) {
                Files.move(
                    newestFile.toPath(),
                    destinationCanonical.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                )
            }

            candidates
                .filter { candidate -> candidate != destinationCanonical && candidate.exists() }
                .forEach { staleFile ->
                    logger.lifecycle("Deleting stale file: ${staleFile.normalizedPath()}")
                    staleFile.delete()
                }
        }
    }
}
