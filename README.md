Avalon
========================================================================

[![CI](https://github.com/lishangbu/avalon/actions/workflows/ci.yml/badge.svg)](https://github.com/lishangbu/avalon/actions/workflows/ci.yml)
[![Deploy Snapshot With Gradle](https://github.com/lishangbu/avalon/actions/workflows/deploy-snapshot.yml/badge.svg)](https://github.com/lishangbu/avalon/actions/workflows/deploy-snapshot.yml)
[![Publish API Docs](https://github.com/lishangbu/avalon/actions/workflows/publish-api-docs.yml/badge.svg)](https://github.com/lishangbu/avalon/actions/workflows/publish-api-docs.yml)
[![Publish DockerHub](https://github.com/lishangbu/avalon/actions/workflows/publish-dockerhub.yml/badge.svg)](https://github.com/lishangbu/avalon/actions/workflows/publish-dockerhub.yml)
[![License](https://img.shields.io/github/license/lishangbu/avalon)](https://github.com/lishangbu/avalon/blob/main/LICENSE)

[![Kotlin](https://img.shields.io/badge/dynamic/toml?url=https://raw.githubusercontent.com/lishangbu/avalon/main/gradle/libs.versions.toml&query=%24.versions.kotlin&label=Kotlin&color=7F52FF&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Spring Boot](https://img.shields.io/badge/dynamic/toml?url=https://raw.githubusercontent.com/lishangbu/avalon/main/gradle/libs.versions.toml&query=%24.versions%5B%27spring-boot%27%5D&label=Spring%20Boot&color=6DB33F&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Jimmer](https://img.shields.io/badge/dynamic/toml?url=https://raw.githubusercontent.com/lishangbu/avalon/main/gradle/libs.versions.toml&query=%24.versions.jimmer&label=Jimmer&color=0E7C86)](https://babyfish-ct.github.io/jimmer-doc/)

## Navigation

[Usage Document Online][1] |
**Server Repository** |
[Admin Client Repository][2] |
[Usage Document Repository][3] |
[API Docs Online][4] |
[Aggregate Test Report][5] |
[Aggregate Coverage Report][6]

[1]: https://lishangbu.github.io/avalon-site/docs

[2]: https://github.com/lishangbu/avalon-admin-ui

[3]: https://github.com/lishangbu/avalon-doc

[4]: https://lishangbu.github.io/avalon-site/api-docs

[5]: https://lishangbu.github.io/avalon-site/quality/test-reports/

[6]: https://lishangbu.github.io/avalon-site/quality/coverage/

## Table of Contents

- [Usage](#usage)
- [Repository Layout](#repository-layout)
- [Quality Reports](#quality-reports)
- [Maintainers](#maintainers)
- [License](#license)

## Usage

To see how the project has been applied, see the [online documents](https://lishangbu.github.io/avalon-site/docs).

The Document is hosted by [vitepress](https://vitepress.dev) ,you must install it yourself if you want to run it
locally.

### IP2Location Starter

`avalon-platform/avalon-ip2location-spring-boot-starter` now exposes the official
`com.ip2location.IP2Location` type directly. The starter only handles Spring Boot
configuration, BIN resource loading, and bean lifecycle.

```kotlin
@Service
class GeoIpService(
    private val ip2Location: IP2Location,
) {
    fun lookup(ip: String): IPResult = ip2Location.ipQuery(ip)
}
```

By default the starter loads `classpath:IP2LOCATION-LITE-DB11.IPV6.BIN`. To refresh the
database from the official IP2Location service, provide a download token and run:

```bash
./gradlew downloadIpData -PipDbDownloadToken=YOUR_TOKEN -PrefreshIpDb=true
```

If the BIN file already exists in the repository, the task reuses the local copy and does
not contact the download service.

## Repository Layout

- `avalon-application`: executable Spring Boot applications and deployment entry points.
- `avalon-modules`: business modules that implement domain behavior.
- `avalon-platform`: reusable platform capabilities shared by business modules.
- `avalon-platform/avalon-web`: shared web response and exception conventions.
- `avalon-platform/avalon-security`: reusable OAuth2 and security building blocks.
- `avalon-platform/avalon-jimmer`: Jimmer integration shared by persistence-facing modules.
- `avalon-platform/avalon-s3-spring-boot-starter`: reusable S3 starter support.
- `avalon-platform/avalon-ip2location-spring-boot-starter`: geo/IP platform integration.

## Quality Reports

Quality reports are published from CI to GitHub Pages:

- Aggregate test report: <https://lishangbu.github.io/avalon-site/quality/test-reports/>
- Aggregate coverage report: <https://lishangbu.github.io/avalon-site/quality/coverage/>
- Publish workflow: <https://github.com/lishangbu/avalon/actions/workflows/publish-quality-reports.yml>

## Maintainers

[ShangBu Li](https://github.com/lishangbu)

## License

[AGPL-3.0](https://opensource.org/license/agpl-v3)

Copyright (c) 2024-present, ShangBu Li
