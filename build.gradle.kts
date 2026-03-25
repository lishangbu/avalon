import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.credentials.PasswordCredentials
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.FileSystemOperations
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFiles
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.api.tasks.testing.TestReport
import org.gradle.jvm.tasks.Jar
import org.gradle.language.base.plugins.LifecycleBasePlugin
import org.gradle.testing.jacoco.plugins.JacocoPluginExtension
import org.gradle.testing.jacoco.tasks.JacocoCoverageVerification
import org.gradle.testing.jacoco.tasks.JacocoReport
import org.gradle.work.DisableCachingByDefault
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import org.springframework.boot.gradle.tasks.bundling.BootBuildImage
import java.math.BigDecimal
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFilePermission
import java.security.KeyPairGenerator
import java.util.*
import javax.inject.Inject

// Shared helpers used by multiple Spring Boot application modules
private fun Project.dockerImageNameProvider() =
    run {
        val serviceName = name
        providers
            .gradleProperty("dockerRepository")
            .zip(providers.gradleProperty("dockerImagePrefix")) { repository, prefix ->
                "$repository/$prefix/$serviceName:latest"
            }
    }

// Root task implementations live here so they can be reused from CI and local development
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

            val candidates =
                repoRoot
                    .walkTopDown()
                    .onEnter { directory -> directory.name !in setOf(".git", ".gradle", "build") }
                    .filter { candidate -> candidate.isFile && candidate.name == dbFileName }
                    .map { candidate -> candidate.canonicalFile }
                    .distinctBy { candidate -> candidate.invariantSeparatorsPath }
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
            logger.lifecycle("Keeping newest file: ${newestFile.invariantSeparatorsPath}")
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
                    logger.lifecycle("Deleting stale file: ${staleFile.invariantSeparatorsPath}")
                    staleFile.delete()
                }
        }
    }
}

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
                    logger.lifecycle("Skipping missing module: ${appDir.invariantSeparatorsPath}")
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

        val targets = existingTargets.joinToString(", ") { targetDir -> targetDir.relativeTo(repoRoot).invariantSeparatorsPath }
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
            backupExistingKeys(targetDir, privateKeyFile, publicKeyFile, backupSuffix, repoRoot)
        }

        privateKeyFile.writeText(generatedKeyPair.privatePem)
        publicKeyFile.writeText(generatedKeyPair.publicPem)
        applyPrivateKeyPermissions(privateKeyFile)
        applyPublicKeyPermissions(publicKeyFile)

        logger.lifecycle("Wrote keys to: ${targetDir.relativeTo(repoRoot).invariantSeparatorsPath}")
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

        logger.lifecycle("Backed up old keys to: ${backupDir.relativeTo(repoRoot).invariantSeparatorsPath}")
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

    private fun hasExistingKeys(targetDir: File): Boolean = targetDir.resolve(PRIVATE_KEY_FILE_NAME).exists() || targetDir.resolve(PUBLIC_KEY_FILE_NAME).exists()

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

// Small cache-friendly utility task used by CI workflows
@DisableCachingByDefault(because = "Prints a configured value to standard output.")
abstract class PrintValueTask : DefaultTask() {
    @get:Input
    abstract val value: Property<String>

    @TaskAction
    fun printValue() {
        println(value.get())
    }
}

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

        siteDirectory.resolve(".nojekyll").writeText("")
    }
}

// Register root plugins once and let subprojects opt into the ones they actually need
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

// Artifact coordinates are driven from gradle.properties for consistent local and CI builds
allprojects {
    group = providers.gradleProperty("group").get()
    version = providers.gradleProperty("version").get()
}

// Shared conventions are attached lazily based on the plugin each module applies
subprojects {
    // Java modules share toolchain, test, publishing, and signing defaults
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
                // Mockito inline mocking needs the agent attached explicitly on newer JDKs
                val mockitoCore = classpath.files.firstOrNull { it.name.startsWith("mockito-core-") }
                if (mockitoCore != null) {
                    jvmArgs("-javaagent:${mockitoCore.absolutePath}")
                }
            }
        }

        apply(plugin = "maven-publish")
        apply(plugin = "signing")

        extensions.configure<PublishingExtension> {
            val centralSnapshotsUsername = providers.gradleProperty("centralSnapshotsUsername").orNull
            val centralSnapshotsPassword = providers.gradleProperty("centralSnapshotsPassword").orNull

            publications {
                create<MavenPublication>("mavenJava") {
                    from(components["java"])
                    pom {
                        name.set(project.name)
                        description.set(project.description ?: project.name)
                        url.set("https://github.com/lishangbu/avalon")
                        licenses {
                            license {
                                name.set("AGPL-V3 License")
                                url.set("https://opensource.org/license/agpl-v3")
                                distribution.set("repo")
                            }
                        }
                        developers {
                            developer {
                                id.set("lisb")
                                name.set("Shangbu Li")
                                email.set("shangshili@hotmail.com")
                            }
                        }
                        scm {
                            connection.set("scm:git:https://github.com/lishangbu/avalon.git")
                            developerConnection.set("scm:git:https://github.com/lishangbu/avalon.git")
                            url.set("https://github.com/lishangbu/avalon")
                        }
                    }
                }
            }
            repositories {
                if (!centralSnapshotsUsername.isNullOrBlank() && !centralSnapshotsPassword.isNullOrBlank()) {
                    maven {
                        name = "centralSnapshots"
                        url =
                            uri(
                                providers
                                    .environmentVariable("MAVEN_CENTRAL_SNAPSHOT_URL")
                                    .orElse("https://central.sonatype.com/repository/maven-snapshots/")
                                    .get(),
                            )
                        credentials(PasswordCredentials::class)
                    }
                }
            }
        }

        extensions.configure<SigningExtension> {
            val signingKey = providers.environmentVariable("GPG_PRIVATE_KEY").orNull
            val signingPassphrase = providers.environmentVariable("GPG_PASSPHRASE").orNull
            if (!signingKey.isNullOrBlank()) {
                useInMemoryPgpKeys(signingKey, signingPassphrase)
                sign(extensions.getByType(PublishingExtension::class.java).publications)
            }
        }
    }

    // Library modules consume the shared BOMs so leaf scripts can omit dependency versions
    pluginManager.withPlugin("java-library") {
        dependencies {
            add("implementation", platform(libs.spring.boot.bom))
            add("testImplementation", platform(libs.spring.boot.bom))
            add("implementation", platform(libs.aws.bom))
            add("testImplementation", platform(libs.aws.bom))
            add("implementation", platform(libs.jimmer.bom))
            add("testImplementation", platform(libs.jimmer.bom))
        }
    }

    // Boot applications inherit BOM alignment and keep image publishing disabled by default
    pluginManager.withPlugin("org.springframework.boot") {
        tasks.withType<BootBuildImage>().configureEach {
            imageName.set(project.dockerImageNameProvider())
            publish.set(false)
        }

        dependencies {
            add("implementation", platform(libs.spring.boot.bom))
            add("testImplementation", platform(libs.spring.boot.bom))
            add("implementation", libs.spring.boot.starter.liquibase)
            add("implementation", platform(libs.aws.bom))
            add("testImplementation", platform(libs.aws.bom))
            add("implementation", platform(libs.jimmer.bom))
            add("testImplementation", platform(libs.jimmer.bom))
        }
    }

    // KSP modules align generated code with the same Jimmer version as runtime dependencies
    pluginManager.withPlugin("com.google.devtools.ksp") {
        dependencies {
            add("ksp", platform(libs.jimmer.bom))
        }
    }

    // Kotlin JVM defaults cover linting, compiler level, and reflection support
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

    // Dokka output is packed into javadocJar to satisfy Maven Central style publication requirements
    pluginManager.withPlugin("org.jetbrains.dokka") {
        rootProject.dependencies.add("dokka", project(path))

        tasks.named<Jar>("javadocJar").configure {
            dependsOn(tasks.named("dokkaGeneratePublicationHtml"))
            from(layout.buildDirectory.dir("dokka/html"))
        }
    }

    // The BOM module follows the same repository and signing rules as published libraries
    pluginManager.withPlugin("java-platform") {
        apply(plugin = "maven-publish")
        apply(plugin = "signing")

        extensions.configure<PublishingExtension> {
            val centralSnapshotsUsername = providers.gradleProperty("centralSnapshotsUsername").orNull
            val centralSnapshotsPassword = providers.gradleProperty("centralSnapshotsPassword").orNull

            publications {
                create<MavenPublication>("mavenBom") {
                    from(components["javaPlatform"])
                    pom {
                        name.set(project.name)
                        description.set(project.description ?: project.name)
                        url.set("https://github.com/lishangbu/avalon")
                        licenses {
                            license {
                                name.set("AGPL-V3 License")
                                url.set("https://opensource.org/license/agpl-v3")
                                distribution.set("repo")
                            }
                        }
                        developers {
                            developer {
                                id.set("lisb")
                                name.set("Shangbu Li")
                                email.set("shangshili@hotmail.com")
                            }
                        }
                        scm {
                            connection.set("scm:git:https://github.com/lishangbu/avalon.git")
                            developerConnection.set("scm:git:https://github.com/lishangbu/avalon.git")
                            url.set("https://github.com/lishangbu/avalon")
                        }
                    }
                }
            }
            repositories {
                if (!centralSnapshotsUsername.isNullOrBlank() && !centralSnapshotsPassword.isNullOrBlank()) {
                    maven {
                        name = "centralSnapshots"
                        url =
                            uri(
                                providers
                                    .environmentVariable("MAVEN_CENTRAL_SNAPSHOT_URL")
                                    .orElse("https://central.sonatype.com/repository/maven-snapshots/")
                                    .get(),
                            )
                        credentials(PasswordCredentials::class)
                    }
                }
            }
        }

        extensions.configure<SigningExtension> {
            val signingKey = providers.environmentVariable("GPG_PRIVATE_KEY").orNull
            val signingPassphrase = providers.environmentVariable("GPG_PASSPHRASE").orNull
            if (!signingKey.isNullOrBlank()) {
                useInMemoryPgpKeys(signingKey, signingPassphrase)
                sign(extensions.getByType(PublishingExtension::class.java).publications)
            }
        }
    }
}

private val coverageExclusions =
    listOf(
        "**/*Application*",
        "**/*Draft*",
        "**/*\$DefaultImpls.class",
        "**/package-info.*",
    )

private fun Project.mainSourceDirectories() =
    listOf(
        layout.projectDirectory.dir("src/main/kotlin"),
        layout.projectDirectory.dir("src/main/java"),
    ).filter { it.asFile.exists() }

private fun Project.mainClassDirectories() =
    listOf(
        layout.buildDirectory.dir("classes/kotlin/main"),
        layout.buildDirectory.dir("classes/java/main"),
    ).map { classesDir ->
        fileTree(classesDir) {
            exclude(coverageExclusions)
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
    classDirectories.from(coverageProjects.flatMap { it.mainClassDirectories() })

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
    classDirectories.from(coverageProjects.flatMap { it.mainClassDirectories() })

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

// Root utility tasks used directly by CI jobs and local setup commands
tasks.register<PrintValueTask>("printVersion") {
    group = "build setup"
    description = "Print the version of this project."
    value.set(providers.gradleProperty("version"))
}

val defaultIpDbFiles = listOf("IP2LOCATION-LITE-DB11.IPV6.BIN")

tasks.register<DownloadIpDataTask>("downloadIpData") {
    group = "build setup"
    description = "Downloads and normalizes IP2Location database files into module resources."

    // Keep the download configurable, but provide a sensible default for routine builds
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

    // CI can overwrite existing keys explicitly; local runs stay safe by default
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
