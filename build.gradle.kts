import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jlleitschuh.gradle.ktlint.KtlintExtension
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.PosixFilePermission
import java.security.KeyPairGenerator
import java.util.*

plugins {
    id("org.jlleitschuh.gradle.ktlint") version "12.1.2"
    id("com.google.devtools.ksp") version "2.3.6" apply false
    id("org.jetbrains.kotlin.jvm") version "2.3.20" apply false
    id("org.jetbrains.kotlin.plugin.spring") version "2.3.20" apply false
    id("org.springframework.boot") version "4.0.4" apply false
    id("org.jetbrains.dokka") version "2.1.0"
}

allprojects {
    group = providers.gradleProperty("group").get()
    version = providers.gradleProperty("version").get()
}

subprojects {
    pluginManager.withPlugin("java") {
        extensions.configure<JavaPluginExtension> {
            toolchain.languageVersion.set(JavaLanguageVersion.of(25))
            withSourcesJar()
            withJavadocJar()
        }

        configurations.named("testCompileOnly") {
            extendsFrom(configurations.getByName("compileOnly"))
        }
        configurations.named("testRuntimeOnly") {
            extendsFrom(configurations.getByName("compileOnly"))
        }

        dependencies {
            add("testImplementation", "org.jetbrains.kotlin:kotlin-test-junit5:2.3.20")
            add("testImplementation", "org.springframework.boot:spring-boot-starter-test")
        }

        tasks.withType<Test>().configureEach {
            useJUnitPlatform()
            maxParallelForks = 1
            jvmArgs("--enable-native-access=ALL-UNNAMED")
            doFirst {
                val mockitoCore = classpath.files.firstOrNull { it.name.startsWith("mockito-core-") }
                if (mockitoCore != null) {
                    jvmArgs("-javaagent:${mockitoCore.absolutePath}")
                }
            }
        }

        apply(plugin = "maven-publish")
        apply(plugin = "signing")

        extensions.configure<PublishingExtension> {
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
                maven {
                    name = "centralSnapshots"
                    url =
                        uri(
                            providers
                                .environmentVariable("MAVEN_CENTRAL_SNAPSHOT_URL")
                                .orElse("https://central.sonatype.com/repository/maven-snapshots/")
                                .get(),
                        )
                    credentials {
                        username = providers.environmentVariable("MAVEN_CENTRAL_USERNAME").orNull
                        password = providers.environmentVariable("MAVEN_CENTRAL_PASSWORD").orNull
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

        tasks.withType<PublishToMavenRepository>().configureEach {
            onlyIf {
                val username = providers.environmentVariable("MAVEN_CENTRAL_USERNAME").orNull
                val password = providers.environmentVariable("MAVEN_CENTRAL_PASSWORD").orNull
                !username.isNullOrBlank() && !password.isNullOrBlank()
            }
        }
    }

    pluginManager.withPlugin("java-library") {
        dependencies {
            add("api", platform("org.springframework.boot:spring-boot-dependencies:4.0.4"))
            add("testImplementation", platform("org.springframework.boot:spring-boot-dependencies:4.0.4"))
            add("api", platform("software.amazon.awssdk:bom:2.41.24"))
            add("testImplementation", platform("software.amazon.awssdk:bom:2.41.24"))
            add("api", platform("org.babyfish.jimmer:jimmer-bom:0.10.6"))
            add("testImplementation", platform("org.babyfish.jimmer:jimmer-bom:0.10.6"))
        }
    }

    pluginManager.withPlugin("org.springframework.boot") {
        dependencies {
            add("implementation", platform("org.springframework.boot:spring-boot-dependencies:4.0.4"))
            add("testImplementation", platform("org.springframework.boot:spring-boot-dependencies:4.0.4"))
            add("implementation", "org.springframework.boot:spring-boot-starter-liquibase")
            add("implementation", platform("software.amazon.awssdk:bom:2.41.24"))
            add("testImplementation", platform("software.amazon.awssdk:bom:2.41.24"))
            add("implementation", platform("org.babyfish.jimmer:jimmer-bom:0.10.6"))
            add("testImplementation", platform("org.babyfish.jimmer:jimmer-bom:0.10.6"))
        }
    }

    pluginManager.withPlugin("com.google.devtools.ksp") {
        dependencies {
            add("ksp", platform("org.babyfish.jimmer:jimmer-bom:0.10.6"))
        }
    }

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
            add("implementation", "org.jetbrains.kotlin:kotlin-reflect:2.3.20")
            add("implementation", "org.jetbrains.kotlin:kotlin-stdlib-jdk8:2.3.20")
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

    pluginManager.withPlugin("org.jetbrains.dokka") {
        rootProject.dependencies.add("dokka", project(path))

        tasks.named<Jar>("javadocJar").configure {
            dependsOn(tasks.named("dokkaGeneratePublicationHtml"))
            from(layout.buildDirectory.dir("dokka/html"))
        }
    }

    pluginManager.withPlugin("java-platform") {
        apply(plugin = "maven-publish")
        apply(plugin = "signing")

        extensions.configure<PublishingExtension> {
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
                maven {
                    name = "centralSnapshots"
                    url =
                        uri(
                            providers
                                .environmentVariable("MAVEN_CENTRAL_SNAPSHOT_URL")
                                .orElse("https://central.sonatype.com/repository/maven-snapshots/")
                                .get(),
                        )
                    credentials {
                        username = providers.environmentVariable("MAVEN_CENTRAL_USERNAME").orNull
                        password = providers.environmentVariable("MAVEN_CENTRAL_PASSWORD").orNull
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

        tasks.withType<PublishToMavenRepository>().configureEach {
            onlyIf {
                val username = providers.environmentVariable("MAVEN_CENTRAL_USERNAME").orNull
                val password = providers.environmentVariable("MAVEN_CENTRAL_PASSWORD").orNull
                !username.isNullOrBlank() && !password.isNullOrBlank()
            }
        }
    }
}

tasks.register("printVersion") {
    doLast {
        println(project.version)
    }
}

tasks.register("downloadIpData") {
    group = "build setup"
    description = "Downloads and normalizes IP2Location database files into module resources."

    doLast {
        // Optional task parameters: keep defaults aligned with the original shell script.
        val dbVersion =
            findProperty("ipDbVersion")
                ?.toString()
                ?.trim()
                ?.takeIf { it.isNotBlank() }
                ?: "2025.12.01"
        val dbFileNames =
            (
                findProperty("ipDbFiles")
                    ?.toString()
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?: "IP2LOCATION-LITE-DB11.IPV6.BIN"
            ).split(',')
                .map { it.trim() }
                .filter { it.isNotBlank() }
                .distinct()

        if (dbFileNames.isEmpty()) {
            throw GradleException("No IP database files configured. Set -PipDbFiles=file1,file2.")
        }

        val destinationDir =
            layout.projectDirectory
                .dir(
                    "avalon-extensions/avalon-ip2location-spring-boot-starter/src/main/resources",
                ).asFile
        destinationDir.mkdirs()

        val urlPrefix = "https://github.com/renfei/ip2location/releases/download/$dbVersion"

        dbFileNames.forEach { dbFileName ->
            val destinationFile = destinationDir.resolve(dbFileName)
            val destinationCanonical = destinationFile.canonicalFile

            // Find same-named files in the repo, then keep only the newest one.
            val candidates =
                fileTree(rootDir) {
                    include("**/$dbFileName")
                    exclude("**/.git/**", "**/.gradle/**", "**/build/**")
                }.files
                    .map { it.canonicalFile }
                    .distinctBy { it.invariantSeparatorsPath }
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

            // Normalize to the canonical destination and delete stale duplicates.
            val newestFile = candidates.maxByOrNull { it.lastModified() } ?: destinationCanonical
            logger.lifecycle("Keeping newest file: ${newestFile.invariantSeparatorsPath}")
            if (newestFile != destinationCanonical) {
                Files.move(
                    newestFile.toPath(),
                    destinationCanonical.toPath(),
                    StandardCopyOption.REPLACE_EXISTING,
                )
            }

            candidates
                .distinctBy { it.invariantSeparatorsPath }
                .filter { it != destinationCanonical && it.exists() }
                .forEach { staleFile ->
                    logger.lifecycle("Deleting stale file: ${staleFile.invariantSeparatorsPath}")
                    staleFile.delete()
                }
        }
    }
}

tasks.register("generateRsaKeys") {
    group = "build setup"
    description = "Generates one RSA key pair and writes it to configured application resource directories."

    doLast {
        val keySize =
            (
                findProperty("keySize")
                    ?.toString()
                    ?.trim()
                    ?.takeIf { it.isNotBlank() }
                    ?: "2048"
            ).toIntOrNull()
                ?: throw GradleException("keySize must be a valid integer.")
        if (keySize < 1024) {
            throw GradleException("keySize must be >= 1024, but was $keySize.")
        }
        val forceOverwrite =
            when (findProperty("force")?.toString()?.trim()?.lowercase()) {
                "true", "1", "yes", "y", "on" -> true
                else -> false
            }

        val privateKeyFileName = "private_key.pem"
        val publicKeyFileName = "public_key.pem"

        val targetDirectories = mutableListOf<File>()
        listOf(
            "avalon-application/avalon-admin-server",
            "avalon-application/avalon-standalone-server",
        ).forEach { appPath ->
            val appDir = rootDir.resolve(appPath)
            if (!appDir.isDirectory) {
                logger.lifecycle("Skipping missing module: ${appDir.invariantSeparatorsPath}")
                return@forEach
            }
            targetDirectories.add(appDir.resolve("src/main/resources/rsa"))
        }

        if (targetDirectories.isEmpty()) {
            throw GradleException("No target application modules found for RSA key generation.")
        }

        val existingTargets =
            targetDirectories.filter { targetDir ->
                targetDir.resolve(privateKeyFileName).exists() || targetDir.resolve(publicKeyFileName).exists()
            }
        if (!forceOverwrite && existingTargets.isNotEmpty()) {
            val targets = existingTargets.joinToString(", ") { it.relativeTo(rootDir).invariantSeparatorsPath }
            throw GradleException(
                "Detected existing key files in: $targets. " +
                    "Re-run with -Pforce=true to overwrite.",
            )
        }

        // Generate one shared RSA keypair for all target applications.
        val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
        keyPairGenerator.initialize(keySize)
        val keyPair = keyPairGenerator.generateKeyPair()

        val pemEncoder = Base64.getMimeEncoder(64, "\n".toByteArray())
        val privatePem =
            buildString {
                appendLine("-----BEGIN PRIVATE KEY-----")
                appendLine(pemEncoder.encodeToString(keyPair.private.encoded))
                appendLine("-----END PRIVATE KEY-----")
            }
        val publicPem =
            buildString {
                appendLine("-----BEGIN PUBLIC KEY-----")
                appendLine(pemEncoder.encodeToString(keyPair.public.encoded))
                appendLine("-----END PUBLIC KEY-----")
            }
        val backupSuffix = System.currentTimeMillis()
        targetDirectories.forEach { targetDir ->
            targetDir.mkdirs()

            val privateKeyFile = targetDir.resolve(privateKeyFileName)
            val publicKeyFile = targetDir.resolve(publicKeyFileName)

            // On force mode, keep a timestamped backup before overwrite.
            if (forceOverwrite && (privateKeyFile.exists() || publicKeyFile.exists())) {
                val backupDir = targetDir.resolve(".rsa_backup_$backupSuffix")
                backupDir.mkdirs()

                if (privateKeyFile.exists()) {
                    Files.move(
                        privateKeyFile.toPath(),
                        backupDir.resolve(privateKeyFileName).toPath(),
                        StandardCopyOption.REPLACE_EXISTING,
                    )
                }
                if (publicKeyFile.exists()) {
                    Files.move(
                        publicKeyFile.toPath(),
                        backupDir.resolve(publicKeyFileName).toPath(),
                        StandardCopyOption.REPLACE_EXISTING,
                    )
                }
                logger.lifecycle("Backed up old keys to: ${backupDir.relativeTo(rootDir).invariantSeparatorsPath}")
            }

            privateKeyFile.writeText(privatePem)
            publicKeyFile.writeText(publicPem)
            val privateKeyPermissions =
                setOf(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                )
            val publicKeyPermissions =
                setOf(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE,
                    PosixFilePermission.GROUP_READ,
                    PosixFilePermission.OTHERS_READ,
                )

            runCatching { Files.setPosixFilePermissions(privateKeyFile.toPath(), privateKeyPermissions) }
                .onFailure {
                    // Fallback for non-POSIX filesystems (e.g. Windows).
                    privateKeyFile.setReadable(false, false)
                    privateKeyFile.setReadable(true, true)
                    privateKeyFile.setWritable(false, false)
                    privateKeyFile.setWritable(true, true)
                }
            runCatching { Files.setPosixFilePermissions(publicKeyFile.toPath(), publicKeyPermissions) }
                .onFailure {
                    publicKeyFile.setReadable(true, false)
                    publicKeyFile.setWritable(true, true)
                }

            logger.lifecycle("Wrote keys to: ${targetDir.relativeTo(rootDir).invariantSeparatorsPath}")
        }
    }
}
