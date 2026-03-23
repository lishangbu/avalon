rootProject.name = "avalon"

enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

pluginManagement {
    repositories {
        maven("https://mirrors.cloud.tencent.com/nexus/repository/maven-public/")
        gradlePluginPortal()
        mavenCentral()
    }
}

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
include(":avalon-extensions:avalon-ip2location-spring-boot-starter")
include(":avalon-extensions:avalon-jimmer-extension")
include(":avalon-extensions:avalon-s3-spring-boot-starter")
include(":avalon-modules:avalon-authorization")
include(":avalon-modules:avalon-dataset:avalon-dataset-controller")
include(":avalon-modules:avalon-dataset:avalon-dataset-model")
include(":avalon-modules:avalon-dataset:avalon-dataset-repository")
include(":avalon-modules:avalon-dataset:avalon-dataset-service")
include(":avalon-modules:avalon-game:avalon-game-battle-engine")
include(":avalon-modules:avalon-game:avalon-game-calculator")
include(":avalon-support:avalon-oauth2-support:avalon-oauth2-authorization-server")
include(":avalon-support:avalon-oauth2-support:avalon-oauth2-common")
include(":avalon-support:avalon-oauth2-support:avalon-oauth2-resource-server")
include(":avalon-support:avalon-web-support")
