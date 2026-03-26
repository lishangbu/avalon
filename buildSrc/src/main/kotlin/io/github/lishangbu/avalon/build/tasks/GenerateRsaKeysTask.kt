package io.github.lishangbu.avalon.build.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFilePermission
import java.security.KeyPairGenerator
import java.util.Base64

private fun File.normalizedPath(): String = path.replace(File.separatorChar, '/')

/**
 * 生成一对 RSA 密钥，并写入所有配置好的应用模块。
 *
 * 这个任务默认采取保守策略，除非显式传入 `-Pforce=true`，
 * 否则不会覆盖已经存在的密钥文件。
 */
@DisableCachingByDefault(because = "Generates RSA key material into project resources.")
abstract class GenerateRsaKeysTask : DefaultTask() {
    private data class GeneratedPemKeyPair(
        val privatePem: String,
        val publicPem: String,
    )

    companion object {
        private const val MINIMUM_RSA_KEY_SIZE = 1024
        private const val RSA_ALGORITHM = "RSA"
        private const val RSA_RESOURCE_DIRECTORY = "src/main/resources/rsa"
        private const val PRIVATE_KEY_FILE_NAME = "private_key.pem"
        private const val PUBLIC_KEY_FILE_NAME = "public_key.pem"
        private const val PRIVATE_KEY_LABEL = "PRIVATE KEY"
        private const val PUBLIC_KEY_LABEL = "PUBLIC KEY"

        private val pemEncoder = Base64.getMimeEncoder(64, "\n".toByteArray())
        private val privateKeyPermissions =
            setOf(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
            )
        private val publicKeyPermissions =
            setOf(
                PosixFilePermission.OWNER_READ,
                PosixFilePermission.OWNER_WRITE,
                PosixFilePermission.GROUP_READ,
                PosixFilePermission.OTHERS_READ,
            )
    }

    @get:Input
    abstract val keySize: Property<Int>

    @get:Input
    abstract val forceOverwrite: Property<Boolean>

    @get:Input
    abstract val applicationPaths: ListProperty<String>

    @get:OutputFiles
    abstract val outputFiles: ConfigurableFileCollection

    @get:Internal
    abstract val repoRootDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val configuredKeySize = keySize.get()
        if (configuredKeySize < MINIMUM_RSA_KEY_SIZE) {
            throw GradleException("keySize must be >= 1024, but was $configuredKeySize.")
        }

        val repoRoot = repoRootDir.get().asFile
        val overwrite = forceOverwrite.get()
        val targetDirectories = resolveTargetDirectories(repoRoot)
        validateExistingKeys(targetDirectories, repoRoot, overwrite)
        val generatedKeyPair = generatePemKeyPair(configuredKeySize)
        val backupSuffix = System.currentTimeMillis()

        targetDirectories.forEach { targetDir ->
            writeKeyPair(
                targetDir = targetDir,
                generatedKeyPair = generatedKeyPair,
                overwrite = overwrite,
                backupSuffix = backupSuffix,
                repoRoot = repoRoot,
            )
        }
    }

    private fun resolveTargetDirectories(repoRoot: File): List<File> {
        val targetDirectories =
            applicationPaths.get().mapNotNull { appPath ->
                val appDir = repoRoot.resolve(appPath)
                if (!appDir.isDirectory) {
                    logger.lifecycle("Skipping missing module: ${appDir.normalizedPath()}")
                    null
                } else {
                    appDir.resolve(RSA_RESOURCE_DIRECTORY)
                }
            }

        if (targetDirectories.isEmpty()) {
            throw GradleException("No target application modules found for RSA key generation.")
        }

        return targetDirectories
    }

    private fun validateExistingKeys(
        targetDirectories: List<File>,
        repoRoot: File,
        overwrite: Boolean,
    ) {
        if (overwrite) {
            return
        }

        val existingTargets = targetDirectories.filter(::hasExistingKeys)
        if (existingTargets.isEmpty()) {
            return
        }

        val targets =
            existingTargets.joinToString(", ") { targetDir ->
                targetDir.relativeTo(repoRoot).normalizedPath()
            }
        throw GradleException(
            "Detected existing key files in: $targets. " +
                "Re-run with -Pforce=true to overwrite.",
        )
    }

    private fun generatePemKeyPair(configuredKeySize: Int): GeneratedPemKeyPair {
        val keyPair =
            KeyPairGenerator
                .getInstance(RSA_ALGORITHM)
                .apply { initialize(configuredKeySize) }
                .generateKeyPair()

        return GeneratedPemKeyPair(
            privatePem = keyPair.private.encoded.toPem(PRIVATE_KEY_LABEL),
            publicPem = keyPair.public.encoded.toPem(PUBLIC_KEY_LABEL),
        )
    }

    private fun ByteArray.toPem(label: String): String =
        buildString {
            appendLine("-----BEGIN $label-----")
            appendLine(pemEncoder.encodeToString(this@toPem))
            appendLine("-----END $label-----")
        }

    private fun writeKeyPair(
        targetDir: File,
        generatedKeyPair: GeneratedPemKeyPair,
        overwrite: Boolean,
        backupSuffix: Long,
        repoRoot: File,
    ) {
        targetDir.mkdirs()

        val privateKeyFile = targetDir.resolve(PRIVATE_KEY_FILE_NAME)
        val publicKeyFile = targetDir.resolve(PUBLIC_KEY_FILE_NAME)

        if (overwrite) {
            // 覆盖模式下会先做带时间戳的备份，避免误替换后无法回退。
            backupExistingKeys(targetDir, privateKeyFile, publicKeyFile, backupSuffix, repoRoot)
        }

        privateKeyFile.writeText(generatedKeyPair.privatePem)
        publicKeyFile.writeText(generatedKeyPair.publicPem)
        applyPrivateKeyPermissions(privateKeyFile)
        applyPublicKeyPermissions(publicKeyFile)

        logger.lifecycle("Wrote keys to: ${targetDir.relativeTo(repoRoot).normalizedPath()}")
    }

    private fun backupExistingKeys(
        targetDir: File,
        privateKeyFile: File,
        publicKeyFile: File,
        backupSuffix: Long,
        repoRoot: File,
    ) {
        if (!privateKeyFile.exists() && !publicKeyFile.exists()) {
            return
        }

        val backupDir = targetDir.resolve(".rsa_backup_$backupSuffix")
        backupDir.mkdirs()

        moveIfExists(privateKeyFile, backupDir.resolve(PRIVATE_KEY_FILE_NAME))
        moveIfExists(publicKeyFile, backupDir.resolve(PUBLIC_KEY_FILE_NAME))

        logger.lifecycle("Backed up old keys to: ${backupDir.relativeTo(repoRoot).normalizedPath()}")
    }

    private fun moveIfExists(
        source: File,
        target: File,
    ) {
        if (!source.exists()) {
            return
        }

        Files.move(
            source.toPath(),
            target.toPath(),
            StandardCopyOption.REPLACE_EXISTING,
        )
    }

    private fun hasExistingKeys(targetDir: File): Boolean =
        targetDir.resolve(PRIVATE_KEY_FILE_NAME).exists() || targetDir.resolve(PUBLIC_KEY_FILE_NAME).exists()

    private fun applyPrivateKeyPermissions(privateKeyFile: File) {
        runCatching { Files.setPosixFilePermissions(privateKeyFile.toPath(), privateKeyPermissions) }
            .onFailure {
                privateKeyFile.setReadable(false, false)
                privateKeyFile.setReadable(true, true)
                privateKeyFile.setWritable(false, false)
                privateKeyFile.setWritable(true, true)
            }
    }

    private fun applyPublicKeyPermissions(publicKeyFile: File) {
        runCatching { Files.setPosixFilePermissions(publicKeyFile.toPath(), publicKeyPermissions) }
            .onFailure {
                publicKeyFile.setReadable(true, false)
                publicKeyFile.setWritable(true, true)
            }
    }
}
