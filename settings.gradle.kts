pluginManagement {
    repositories {
        maven("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/")
        gradlePluginPortal()
        mavenCentral()
    }

    plugins {
        id("org.danilopianini.gradle-pre-commit-git-hooks") version "2.1.12"
    }
}

plugins {
    id("org.danilopianini.gradle-pre-commit-git-hooks")
}

gitHooks {
    preCommit {
        tasks("ktlintCheck")
    }
    createHooks(true)
}

rootProject.name = "avalon"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        maven("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/")
        mavenCentral()
        maven("https://central.sonatype.com/repository/maven-snapshots/")
    }
}
include(":avalon-application:avalon-admin-server")
include(":avalon-application:avalon-standalone-server")
include(":avalon-modules:avalon-authorization")
include(":avalon-modules:avalon-dataset:avalon-dataset-controller")
include(":avalon-modules:avalon-dataset:avalon-dataset-model")
include(":avalon-modules:avalon-dataset:avalon-dataset-repository")
include(":avalon-modules:avalon-dataset:avalon-dataset-service")
include(":avalon-modules:avalon-game:avalon-game-battle-engine")
include(":avalon-modules:avalon-game:avalon-game-calculator")
// Platform modules provide reusable technical capabilities to business modules.
include(":avalon-platform:avalon-jimmer")
include(":avalon-platform:avalon-idempotent-spring-boot-starter")
include(":avalon-platform:avalon-ip2location-spring-boot-starter")
include(":avalon-platform:avalon-security:avalon-oauth2-authorization-server")
include(":avalon-platform:avalon-security:avalon-oauth2-common")
include(":avalon-platform:avalon-security:avalon-oauth2-resource-server")
include(":avalon-platform:avalon-s3-spring-boot-starter")
include(":avalon-platform:avalon-web")
