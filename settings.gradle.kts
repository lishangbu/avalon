import java.time.Duration

pluginManagement {
    repositories {
        gradlePluginPortal()
        mavenCentral()
    }
}

plugins {
    id("com.gradleup.nmcp.settings") version "1.5.0"
}

val mavenCentralUsername =
    providers.environmentVariable("MAVEN_CENTRAL_USERNAME")
        .orElse(providers.gradleProperty("mavenCentralUsername"))
        .orNull
val mavenCentralPassword =
    providers.environmentVariable("MAVEN_CENTRAL_PASSWORD")
        .orElse(providers.gradleProperty("mavenCentralPassword"))
        .orNull

nmcpSettings {
    // 正式 release 走 Central Portal 聚合发布；凭据缺失时保持本地开发无感。
    if (!mavenCentralUsername.isNullOrBlank() && !mavenCentralPassword.isNullOrBlank()) {
        centralPortal {
            username = mavenCentralUsername
            password = mavenCentralPassword
            publishingType = "AUTOMATIC"
            validationTimeout = Duration.ofMinutes(30)
        }
    }
}

buildCache {
    local {
        directory = rootDir.resolve(".gradle/build-cache")
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        mavenCentral()
    }
}

rootProject.name = "avalon"

include(":apps:avalon-app")
include(":modules:shared-kernel")
include(":modules:shared-application")
include(":modules:shared-infra")
include(":modules:identity-access")
include(":modules:catalog")
include(":modules:player")
include(":modules:battle")
