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
import java.util.zip.ZipInputStream

private fun File.normalizedPath(): String = path.replace(File.separatorChar, '/')

private const val OFFICIAL_IP2LOCATION_DOWNLOAD_URL = "https://www.ip2location.com/download"

internal fun inferOfficialIp2LocationPackageCode(dbFileName: String): String? {
    val match = Regex("""^IP2LOCATION-LITE-(DB\d+)(\.IPV6)?\.BIN$""").matchEntire(dbFileName) ?: return null
    val dbCode = match.groupValues[1]
    val ipv6Suffix = if (match.groupValues[2].isNotEmpty()) "IPV6" else ""
    return "${dbCode}LITEBIN$ipv6Suffix"
}

internal fun officialIp2LocationDownloadUrl(
    token: String,
    packageCode: String,
): String = "$OFFICIAL_IP2LOCATION_DOWNLOAD_URL?token=$token&file=$packageCode"

internal fun extractFileFromZipStream(
    zipInputStream: ZipInputStream,
    expectedFileName: String,
    destinationFile: File,
): Boolean {
    while (true) {
        val entry = zipInputStream.nextEntry ?: return false
        if (!entry.isDirectory && entry.name.substringAfterLast('/') == expectedFileName) {
            Files.copy(zipInputStream, destinationFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            zipInputStream.closeEntry()
            return true
        }
        zipInputStream.closeEntry()
    }
}

/**
 * 下载并归并 IP2Location 数据文件到统一的资源目录。
 *
 * 任务会优先复用仓库里已经存在的最新文件，只有在找不到可复用副本时
 * 才重新下载，这样本地开发和 CI 恢复缓存的成本都更低。
 *
 * 当需要重新下载时，会使用 IP2Location 官方下载接口，
 * 因此需要通过 Gradle 属性或环境变量提供下载 token。
 */
@DisableCachingByDefault(because = "Downloads external IP data into project resources.")
abstract class DownloadIpDataTask : DefaultTask() {
    @get:Input
    abstract val dbFileNames: ListProperty<String>

    @get:Input
    abstract val forceRefresh: Property<Boolean>

    @get:OutputDirectory
    abstract val destinationDir: DirectoryProperty

    @get:Internal
    abstract val repoRootDir: DirectoryProperty

    @get:Internal
    abstract val downloadToken: Property<String>

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
        val shouldForceRefresh = forceRefresh.orNull == true
        destinationRoot.mkdirs()

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

            if (!shouldForceRefresh && candidates.isNotEmpty()) {
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
                return@forEach
            }

            val token =
                downloadToken
                    .orNull
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?: throw GradleException(
                        buildString {
                            append("Missing IP2Location download token. ")
                            append("Set -PipDbDownloadToken=<token> or IP2LOCATION_DOWNLOAD_TOKEN when downloading official data files.")
                        },
                    )

            val packageCode =
                inferOfficialIp2LocationPackageCode(dbFileName)
                    ?: throw GradleException(
                        "Unsupported IP database file name: $dbFileName. Expected official IP2Location BIN file names like IP2LOCATION-LITE-DB11.IPV6.BIN.",
                    )
            val downloadUrl = officialIp2LocationDownloadUrl(token, packageCode)
            logger.lifecycle("Downloading official IP2Location package: $packageCode")
            URI(downloadUrl).toURL().openStream().use { inputStream ->
                ZipInputStream(inputStream.buffered()).use { zipInputStream ->
                    val extracted = extractFileFromZipStream(zipInputStream, dbFileName, destinationCanonical)
                    if (!extracted) {
                        throw GradleException(
                            "Official IP2Location archive for $packageCode does not contain $dbFileName. Check the token and package code.",
                        )
                    }
                }
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
