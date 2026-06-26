package io.github.lishangbu.s3

import software.amazon.awssdk.awscore.exception.AwsServiceException
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.HeadObjectResponse
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.S3Exception
import software.amazon.awssdk.services.s3.model.S3Object
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest

/**
 * 基于 AWS SDK v2 的 [S3Operations] 默认实现。
 */
class DefaultS3Operations(
	private val s3Client: S3Client,
	private val s3Presigner: S3Presigner,
	private val settings: S3ClientSettings,
) : S3Operations {
	override fun putObject(command: S3PutObjectCommand): S3PutObjectResult =
		translate("putObject", command.key) {
			val request = PutObjectRequest.builder()
				.bucket(settings.bucket)
				.key(resolvedKey(command.key))
				.contentType(command.contentType)
				.metadata(command.metadata)
				.build()
			val response = s3Client.putObject(request, RequestBody.fromBytes(command.content))
			S3PutObjectResult(command.key, response.eTag(), response.versionId())
		}

	override fun putObject(command: S3PutObjectStreamCommand): S3PutObjectResult =
		translate("putObject", command.key) {
			val request = PutObjectRequest.builder()
				.bucket(settings.bucket)
				.key(resolvedKey(command.key))
				.contentType(command.contentType)
				.metadata(command.metadata)
				.build()
			val response = s3Client.putObject(
				request,
				RequestBody.fromInputStream(command.content, command.contentLength),
			)
			S3PutObjectResult(command.key, response.eTag(), response.versionId())
		}

	override fun getObject(key: S3ObjectKey): S3ObjectContent =
		translate("getObject", key) {
			val request = GetObjectRequest.builder()
				.bucket(settings.bucket)
				.key(resolvedKey(key))
				.build()
			val responseBytes = s3Client.getObjectAsBytes(request)
			val response = responseBytes.response()
			S3ObjectContent(
				key = key,
				content = responseBytes.asByteArray(),
				contentType = response.contentType(),
				metadata = response.metadata().orEmpty(),
				eTag = response.eTag(),
				contentLength = response.contentLength(),
			)
		}

	override fun getObjectStream(key: S3ObjectKey): S3ObjectStream =
		translate("getObject", key) {
			val request = GetObjectRequest.builder()
				.bucket(settings.bucket)
				.key(resolvedKey(key))
				.build()
			val responseStream = s3Client.getObject(request)
			val response = responseStream.response()
			S3ObjectStream(
				key = key,
				content = responseStream,
				contentType = response.contentType(),
				metadata = response.metadata().orEmpty(),
				eTag = response.eTag(),
				contentLength = response.contentLength(),
				lastModified = response.lastModified(),
			)
		}

	override fun headObject(key: S3ObjectKey): S3ObjectMetadata =
		translate("headObject", key) {
			val request = HeadObjectRequest.builder()
				.bucket(settings.bucket)
				.key(resolvedKey(key))
				.build()
			s3Client.headObject(request).toMetadata(key)
		}

	override fun deleteObject(key: S3ObjectKey) {
		translate("deleteObject", key) {
			val request = DeleteObjectRequest.builder()
				.bucket(settings.bucket)
				.key(resolvedKey(key))
				.build()
			s3Client.deleteObject(request)
		}
	}

	override fun objectExists(key: S3ObjectKey): Boolean =
		try {
			headObject(key)
			true
		} catch (ex: S3ObjectNotFoundException) {
			false
		}

	override fun listObjects(): S3ObjectPage =
		listObjects(S3ListObjectsCommand())

	override fun listObjects(command: S3ListObjectsCommand): S3ObjectPage =
		translate("listObjects") {
			val requestBuilder = ListObjectsV2Request.builder()
				.bucket(settings.bucket)
				.maxKeys(command.maxKeys)
			resolvedPrefix(command.prefix)?.let(requestBuilder::prefix)
			command.continuationToken?.let(requestBuilder::continuationToken)
			val response = s3Client.listObjectsV2(requestBuilder.build())
			S3ObjectPage(
				objects = response.contents().map { it.toSummary() },
				nextContinuationToken = response.nextContinuationToken(),
				truncated = response.isTruncated == true,
			)
		}

	override fun createPresignedGetUrl(key: S3ObjectKey): S3PresignedUrl =
		createPresignedGetUrl(S3PresignGetObjectCommand(key, settings.defaultPresignTtl))

	override fun createPresignedGetUrl(command: S3PresignGetObjectCommand): S3PresignedUrl =
		translate("presignGetObject", command.key) {
			val getObjectRequest = GetObjectRequest.builder()
				.bucket(settings.bucket)
				.key(resolvedKey(command.key))
				.build()
			val presignRequest = GetObjectPresignRequest.builder()
				.signatureDuration(command.ttl)
				.getObjectRequest(getObjectRequest)
				.build()
			val presignedRequest = s3Presigner.presignGetObject(presignRequest)
			S3PresignedUrl(
				key = command.key,
				url = presignedRequest.url().toURI(),
				method = presignedRequest.httpRequest().method().name,
				expiresAt = presignedRequest.expiration(),
				headers = presignedRequest.httpRequest().headers(),
			)
		}

	override fun createPresignedPutUrl(key: S3ObjectKey): S3PresignedUrl =
		createPresignedPutUrl(S3PresignPutObjectCommand(key, settings.defaultPresignTtl))

	override fun createPresignedPutUrl(command: S3PresignPutObjectCommand): S3PresignedUrl =
		translate("presignPutObject", command.key) {
			val putObjectRequest = PutObjectRequest.builder()
				.bucket(settings.bucket)
				.key(resolvedKey(command.key))
				.contentType(command.contentType)
				.metadata(command.metadata)
				.build()
			val presignRequest = PutObjectPresignRequest.builder()
				.signatureDuration(command.ttl)
				.putObjectRequest(putObjectRequest)
				.build()
			val presignedRequest = s3Presigner.presignPutObject(presignRequest)
			S3PresignedUrl(
				key = command.key,
				url = presignedRequest.url().toURI(),
				method = presignedRequest.httpRequest().method().name,
				expiresAt = presignedRequest.expiration(),
				headers = presignedRequest.httpRequest().headers(),
			)
		}

	private fun resolvedKey(key: S3ObjectKey): String {
		val prefix = settings.keyPrefix.trim('/')
		return if (prefix.isBlank()) key.value else "$prefix/${key.value}"
	}

	private fun resolvedPrefix(prefix: S3ObjectKey?): String? {
		val settingsPrefix = settings.keyPrefix.trim('/')
		if (prefix == null) {
			return settingsPrefix.takeIf(String::isNotBlank)?.let { "$it/" }
		}
		return if (settingsPrefix.isBlank()) prefix.value else "$settingsPrefix/${prefix.value}"
	}

	private fun relativeKey(key: String): S3ObjectKey {
		val prefix = settings.keyPrefix.trim('/')
		val relativeValue = if (prefix.isBlank()) key else key.removePrefix("$prefix/")
		return S3ObjectKey.of(relativeValue)
	}

	private fun HeadObjectResponse.toMetadata(key: S3ObjectKey): S3ObjectMetadata =
		S3ObjectMetadata(
			key = key,
			contentType = contentType(),
			metadata = metadata().orEmpty(),
			eTag = eTag(),
			contentLength = contentLength(),
			lastModified = lastModified(),
		)

	private fun S3Object.toSummary(): S3ObjectSummary =
		S3ObjectSummary(
			key = relativeKey(key()),
			eTag = eTag(),
			size = size(),
			lastModified = lastModified(),
		)

	private fun <T> translate(
		action: String,
		key: S3ObjectKey? = null,
		block: () -> T,
	): T =
		try {
			block()
		} catch (ex: NoSuchKeyException) {
			throw translatedNotFound(action, key, ex)
		} catch (ex: S3Exception) {
			throw when (ex.statusCode()) {
				404 -> translatedNotFound(action, key, ex)
				403 -> translatedAccessDenied(action, key, ex)
				else -> translatedServiceFailure(action, key, ex)
			}
		} catch (ex: AwsServiceException) {
			throw translatedServiceFailure(action, key, ex)
		}

	private fun translatedNotFound(
		action: String,
		key: S3ObjectKey?,
		ex: AwsServiceException,
	): S3StorageException =
		if (key == null) {
			translatedServiceFailure(action, null, ex)
		} else {
			S3ObjectNotFoundException(
				key = key,
				cause = ex,
				action = action,
				statusCode = ex.safeStatusCode(),
				awsErrorCode = ex.safeAwsErrorCode(),
				requestId = ex.safeRequestId(),
			)
		}

	private fun translatedAccessDenied(
		action: String,
		key: S3ObjectKey?,
		ex: AwsServiceException,
	): S3AccessDeniedException =
		S3AccessDeniedException(
			action = action,
			key = key,
			cause = ex,
			statusCode = ex.safeStatusCode(),
			awsErrorCode = ex.safeAwsErrorCode(),
			requestId = ex.safeRequestId(),
		)

	private fun translatedServiceFailure(
		action: String,
		key: S3ObjectKey?,
		ex: AwsServiceException,
	): S3ServiceException =
		S3ServiceException(
			action = action,
			key = key,
			cause = ex,
			statusCode = ex.safeStatusCode(),
			awsErrorCode = ex.safeAwsErrorCode(),
			requestId = ex.safeRequestId(),
		)

	private fun AwsServiceException.safeStatusCode(): Int? =
		statusCode().takeIf { it > 0 }

	private fun AwsServiceException.safeAwsErrorCode(): String? =
		awsErrorDetails()?.errorCode()

	private fun AwsServiceException.safeRequestId(): String? =
		requestId()
}
