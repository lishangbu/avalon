package io.github.lishangbu.avalon.gradle.support

import org.gradle.api.Project
import org.gradle.api.credentials.PasswordCredentials
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.plugins.signing.SigningExtension

private const val projectGithubUrl = "https://github.com/lishangbu/avalon"
private const val projectGitConnection = "scm:git:$projectGithubUrl.git"
private const val projectLicenseUrl = "https://opensource.org/license/agpl-v3"
private const val centralSnapshotsDefaultUrl = "https://central.sonatype.com/repository/maven-snapshots/"

/**
 * 为所有发布物填充共享的 POM 元数据，避免每个模块重复维护许可证、SCM 与开发者信息。
 */
fun MavenPom.configurePomMetadata(project: Project) {
    name.set(project.name)
    description.set(project.description ?: project.name)
    url.set(projectGithubUrl)
    licenses {
        license {
            name.set("AGPL-V3 License")
            url.set(projectLicenseUrl)
            distribution.set("repo")
        }
    }
    developers {
        developer {
            id.set("lishangbu")
            name.set("ShangBu Li")
        }
    }
    scm {
        connection.set(projectGitConnection)
        developerConnection.set(projectGitConnection)
        url.set(projectGithubUrl)
    }
}

/**
 * 仅在 CI 注入了快照仓库凭据时注册 Maven Central snapshots 仓库。
 *
 * 正式 release 由 `settings.gradle.kts` 里的 Central Portal 聚合任务负责，
 * 这里保留的是 `main` 分支快照直推所需的最小仓库配置。
 */
fun PublishingExtension.configureCentralSnapshotsRepository(project: Project) {
    val centralSnapshotsUsername = project.providers.gradleProperty("centralSnapshotsUsername").orNull
    val centralSnapshotsPassword = project.providers.gradleProperty("centralSnapshotsPassword").orNull
    if (!centralSnapshotsUsername.isNullOrBlank() && !centralSnapshotsPassword.isNullOrBlank()) {
        repositories {
            maven {
                name = "centralSnapshots"
                url =
                    project.uri(
                        project.providers
                            .environmentVariable("MAVEN_CENTRAL_SNAPSHOT_URL")
                            .orElse(centralSnapshotsDefaultUrl)
                            .get(),
                    )
                credentials(PasswordCredentials::class.java)
            }
        }
    }
}

/**
 * 仅在环境变量齐备时启用内存中的 PGP 签名，让本地开发与 CI 发布都能共用同一套构建逻辑。
 */
fun SigningExtension.configureSigning(
    project: Project,
    publishing: PublishingExtension,
) {
    val signingKey = project.providers.environmentVariable("GPG_PRIVATE_KEY").orNull
    val signingPassphrase = project.providers.environmentVariable("GPG_PASSPHRASE").orNull
    if (!signingKey.isNullOrBlank()) {
        useInMemoryPgpKeys(signingKey, signingPassphrase)
        sign(publishing.publications)
    }
}

/**
 * 把统一发布约定接到具体组件上。
 */
fun Project.configurePublication(
    publicationName: String,
    componentName: String,
) {
    pluginManager.apply("maven-publish")
    pluginManager.apply("signing")

    val publishing = extensions.getByType(PublishingExtension::class.java)
    val publication = publishing.publications.create(publicationName, MavenPublication::class.java)
    publication.from(components.getByName(componentName))
    publication.pom {
        configurePomMetadata(this@configurePublication)
    }
    publishing.configureCentralSnapshotsRepository(this)

    extensions.configure(SigningExtension::class.java) {
        configureSigning(this@configurePublication, publishing)
    }
}
