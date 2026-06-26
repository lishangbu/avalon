# S3 Spring Boot Starter

S3 starter provides a small Spring Boot integration layer around AWS SDK v2 S3.
It auto-configures:

- `S3Client`
- `S3Presigner`
- `S3ClientSettings`
- `S3Operations`

The starter is dependency-only. Applications should depend on this module, while the
core and autoconfigure modules stay publishable as separate artifacts.

## Dependency

```kotlin
dependencies {
    implementation("io.github.lishangbu:s3-spring-boot-starter:0.0.1-SNAPSHOT")
}
```

For local verification before a remote repository is configured:

```bash
./gradlew :s3-core:publishToMavenLocal \
    :s3-spring-boot-autoconfigure:publishToMavenLocal \
    :s3-spring-boot-starter:publishToMavenLocal
```

## Configuration

Use the AWS SDK default credentials chain in production when the runtime can provide
credentials through IAM role, environment variables, or shared AWS config:

```yaml
s3:
  enabled: true
  bucket: app-assets
  region: us-east-1
```

Use explicit access keys only through environment variables or another secret source.
Do not commit access keys or secret keys to the repository:

```yaml
s3:
  enabled: true
  bucket: app-assets
  region: us-east-1
  credentials:
    access-key: ${S3_ACCESS_KEY}
    secret-key: ${S3_SECRET_KEY}
```

For MinIO or another private S3-compatible endpoint, configure the endpoint and
usually enable path-style access:

```yaml
s3:
  enabled: true
  bucket: app-assets
  region: us-east-1
  endpoint: http://localhost:9000
  path-style-access-enabled: true
  key-prefix: dev
  presign:
    default-ttl: 15m
  credentials:
    access-key: ${S3_ACCESS_KEY:minioadmin}
    secret-key: ${S3_SECRET_KEY:minioadmin}
```

When `s3.enabled=true`, `s3.bucket` must be non-blank. Explicit credentials must
provide `access-key` and `secret-key` together, and `s3.presign.default-ttl` must
be greater than zero.

## Usage

```kotlin
import io.github.lishangbu.s3.S3ListObjectsCommand
import io.github.lishangbu.s3.S3ObjectKey
import io.github.lishangbu.s3.S3Operations
import io.github.lishangbu.s3.S3PutObjectCommand
import io.github.lishangbu.s3.S3PutObjectStreamCommand
import org.springframework.stereotype.Service
import java.io.InputStream

@Service
class AvatarStorage(
    private val s3Operations: S3Operations,
) {
    fun save(userId: Long, content: ByteArray) {
        s3Operations.putObject(
            S3PutObjectCommand(
                key = S3ObjectKey.of("avatars/$userId.png"),
                content = content,
                contentType = "image/png",
            ),
        )
    }

    fun temporaryDownloadUrl(userId: Long) =
        s3Operations.createPresignedGetUrl(
            S3ObjectKey.of("avatars/$userId.png"),
        )

    fun avatarMetadata(userId: Long) =
        s3Operations.headObject(S3ObjectKey.of("avatars/$userId.png"))

    fun listAvatarObjects() =
        s3Operations.listObjects(
            S3ListObjectsCommand(prefix = S3ObjectKey.of("avatars/")),
        )

    fun saveLargeAvatar(userId: Long, content: InputStream, contentLength: Long) {
        s3Operations.putObject(
            S3PutObjectStreamCommand(
                key = S3ObjectKey.of("avatars/$userId-original.png"),
                content = content,
                contentLength = contentLength,
                contentType = "image/png",
            ),
        )
    }

    fun openLargeAvatar(userId: Long) =
        s3Operations.getObjectStream(
            S3ObjectKey.of("avatars/$userId-original.png"),
        )
}
```

Use command objects when a call needs custom TTL, content type, metadata, or streaming
content. Close `S3ObjectStream` after reading, for example with Kotlin `use`.

## Tests

The autoconfigure module uses Testcontainers with MinIO to verify real S3-compatible
put, get, stream, head, list, presign, and delete behavior:

```bash
./gradlew :s3-core:test :s3-spring-boot-autoconfigure:test
```

## Publishing

The three S3 modules apply the shared `backend.publishable-library` Gradle convention.
It creates the `mavenJava` publication, sources jar, Javadoc jar, and a local build
repository named `localBuild`.

For a full local repository layout:

```bash
./gradlew :s3-core:publishAllPublicationsToLocalBuildRepository \
    :s3-spring-boot-autoconfigure:publishAllPublicationsToLocalBuildRepository \
    :s3-spring-boot-starter:publishAllPublicationsToLocalBuildRepository
```

Signing is opt-in for now. Provide `signingInMemoryKey` and
`signingInMemoryKeyPassword` as Gradle properties, or use
`SIGNING_IN_MEMORY_KEY` and `SIGNING_IN_MEMORY_KEY_PASSWORD` environment variables.

A remote release still needs an explicit target repository decision, repository
credentials, legal metadata such as license and SCM, and a non-SNAPSHOT version.
