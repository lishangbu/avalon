package io.github.lishangbu.avalon.build.support

import org.gradle.api.Project
import org.gradle.api.credentials.PasswordCredentials
import org.gradle.api.publish.PublishingExtension
import org.gradle.api.publish.maven.MavenPom
import org.gradle.api.publish.maven.MavenPublication
import org.gradle.plugins.signing.SigningExtension

private val projectGithubUrl = "https://github.com/lishangbu/avalon"
private val projectGitConnection = "scm:git:$projectGithubUrl.git"
private val projectLicenseUrl = "https://opensource.org/license/agpl-v3"
private val centralSnapshotsDefaultUrl = "https://central.sonatype.com/repository/maven-snapshots/"

/**
 * 为所有发布物统一填充共享的 POM 元数据。
 *
 * 这部分集中维护后，根构建脚本就不需要在每一种 publication 上
 * 重复声明许可证、SCM 和开发者信息。
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
            id.set("lisb")
            name.set("Shangbu Li")
            email.set("shangshili@hotmail.com")
        }
    }
    scm {
        connection.set(projectGitConnection)
        developerConnection.set(projectGitConnection)
        url.set(projectGithubUrl)
    }
}

/**
 * 仅在凭据存在时注册 Sonatype snapshots 仓库。
 *
 * 这样开源贡献者本地构建时不需要额外发布密钥，而 CI 和发布环境
 * 只要注入对应属性就能自动启用快照发布。
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
 * 仅在环境变量齐备时启用内存中的 PGP 签名。
 *
 * 签名保持可选，让本地开发过程尽量无感；而 CI 仍然可以在需要时
 * 产出可发布的已签名制品。
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
 * 把统一的发布约定应用到具体组件上。
 *
 * 普通 JVM 模块的 `java` 组件和平台/BOM 模块的 `javaPlatform` 组件
 * 都复用这里的逻辑，这样根脚本只需要表达“发布哪个组件”，不用再关心
 * 重复的 publication 装配细节。
 */
fun Project.configurePublication(
    publicationName: String,
    componentName: String,
) {
    pluginManager.apply("maven-publish")
    pluginManager.apply("signing")

    val publishing = extensions.getByType(PublishingExtension::class.java)
    val publication =
        publishing.publications.create(publicationName, MavenPublication::class.java)
    publication.from(components.getByName(componentName))
    publication.pom {
        configurePomMetadata(project)
    }
    publishing.configureCentralSnapshotsRepository(this)

    extensions.configure(SigningExtension::class.java) {
        configureSigning(this@configurePublication, publishing)
    }
}
