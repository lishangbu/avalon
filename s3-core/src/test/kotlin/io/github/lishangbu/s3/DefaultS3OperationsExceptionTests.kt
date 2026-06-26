package io.github.lishangbu.s3

import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.catchThrowable
import org.mockito.ArgumentMatchers.any
import org.mockito.Mockito.`when`
import org.mockito.Mockito.mock
import software.amazon.awssdk.awscore.exception.AwsErrorDetails
import software.amazon.awssdk.core.sync.RequestBody
import software.amazon.awssdk.services.s3.S3Client
import software.amazon.awssdk.services.s3.model.GetObjectRequest
import software.amazon.awssdk.services.s3.model.HeadObjectRequest
import software.amazon.awssdk.services.s3.model.ListObjectsV2Request
import software.amazon.awssdk.services.s3.model.NoSuchKeyException
import software.amazon.awssdk.services.s3.model.PutObjectRequest
import software.amazon.awssdk.services.s3.model.S3Exception
import software.amazon.awssdk.services.s3.presigner.S3Presigner
import kotlin.test.Test

/**
 * 验证 S3 SDK 异常会被转换为 starter 对外暴露的诊断异常。
 *
 * 这些用例重点保护异常里的 action、key、状态码和请求标识，同时确认消息不会泄露 bucket、
 * 凭证或对象内容等敏感配置。
 */
class DefaultS3OperationsExceptionTests {
	@Test
	fun `get object translates no such key into not found exception`() {
		val s3Client = mock(S3Client::class.java)
		val operations = operations(s3Client)
		`when`(s3Client.getObjectAsBytes(any(GetObjectRequest::class.java)))
			.thenThrow(noSuchKeyException())

		val exception = thrownBy<S3ObjectNotFoundException> {
			operations.getObject(KEY)
		}

		assertThat(exception.action).isEqualTo("getObject")
		assertThat(exception.key).isEqualTo(KEY)
		assertThat(exception.statusCode).isEqualTo(NOT_FOUND_STATUS)
		assertThat(exception.awsErrorCode).isEqualTo("NoSuchKey")
		assertThat(exception.requestId).isEqualTo("request-404")
	}

	@Test
	fun `head object translates access denied into access denied exception`() {
		val s3Client = mock(S3Client::class.java)
		val operations = operations(s3Client)
		`when`(s3Client.headObject(any(HeadObjectRequest::class.java)))
			.thenThrow(s3Exception(FORBIDDEN_STATUS, "AccessDenied", "request-403"))

		val exception = thrownBy<S3AccessDeniedException> {
			operations.headObject(KEY)
		}

		assertThat(exception.action).isEqualTo("headObject")
		assertThat(exception.key).isEqualTo(KEY)
		assertThat(exception.statusCode).isEqualTo(FORBIDDEN_STATUS)
		assertThat(exception.awsErrorCode).isEqualTo("AccessDenied")
		assertThat(exception.requestId).isEqualTo("request-403")
	}

	@Test
	fun `put object translates service failure into service exception without sensitive configuration`() {
		val s3Client = mock(S3Client::class.java)
		val operations = operations(s3Client)
		`when`(s3Client.putObject(any(PutObjectRequest::class.java), any(RequestBody::class.java)))
			.thenThrow(s3Exception(SERVER_ERROR_STATUS, "InternalError", "request-500"))

		val exception = thrownBy<S3ServiceException> {
			operations.putObject(
				S3PutObjectCommand(
					key = KEY,
					content = "sensitive-content".toByteArray(),
					contentType = "text/plain",
				),
			)
		}

		assertThat(exception.action).isEqualTo("putObject")
		assertThat(exception.key).isEqualTo(KEY)
		assertThat(exception.statusCode).isEqualTo(SERVER_ERROR_STATUS)
		assertThat(exception.awsErrorCode).isEqualTo("InternalError")
		assertThat(exception.requestId).isEqualTo("request-500")
		assertThat(exception.message)
			.doesNotContain(BUCKET, "sensitive-content", "access-key", "secret-key")
	}

	@Test
	fun `list objects translates service failure without object key`() {
		val s3Client = mock(S3Client::class.java)
		val operations = operations(s3Client)
		`when`(s3Client.listObjectsV2(any(ListObjectsV2Request::class.java)))
			.thenThrow(s3Exception(SERVICE_UNAVAILABLE_STATUS, "SlowDown", "request-list"))

		val exception = thrownBy<S3ServiceException> {
			operations.listObjects()
		}

		assertThat(exception.action).isEqualTo("listObjects")
		assertThat(exception.key).isNull()
		assertThat(exception.statusCode).isEqualTo(SERVICE_UNAVAILABLE_STATUS)
		assertThat(exception.awsErrorCode).isEqualTo("SlowDown")
		assertThat(exception.requestId).isEqualTo("request-list")
	}

	/**
	 * 使用真实的 [DefaultS3Operations] 和模拟的 SDK client，隔离网络调用后只验证异常翻译逻辑。
	 */
	private fun operations(s3Client: S3Client): S3Operations =
		DefaultS3Operations(
			s3Client = s3Client,
			s3Presigner = mock(S3Presigner::class.java),
			settings = S3ClientSettings(bucket = BUCKET, keyPrefix = "private-prefix"),
		)

	private fun noSuchKeyException(): NoSuchKeyException =
		NoSuchKeyException.builder()
			.statusCode(NOT_FOUND_STATUS)
			.awsErrorDetails(awsErrorDetails("NoSuchKey"))
			.requestId("request-404")
			.message("object missing")
			.build()

	private fun s3Exception(statusCode: Int, errorCode: String, requestId: String): S3Exception =
		S3Exception.builder()
			.statusCode(statusCode)
			.awsErrorDetails(awsErrorDetails(errorCode))
			.requestId(requestId)
			.message("remote service failure")
			.build() as S3Exception

	private fun awsErrorDetails(errorCode: String): AwsErrorDetails =
		AwsErrorDetails.builder()
			.errorCode(errorCode)
			.serviceName("S3")
			.build()

	/**
	 * 将 AssertJ 捕获到的异常保持为强类型，便于后续断言诊断字段。
	 */
	private inline fun <reified T : Throwable> thrownBy(noinline block: () -> Unit): T {
		val throwable = catchThrowable(block)
		assertThat(throwable).isInstanceOf(T::class.java)
		return T::class.java.cast(throwable)
	}

	private companion object {
		private val KEY = S3ObjectKey.of("diagnostics/object.txt")
		private const val BUCKET = "private-bucket"
		private const val NOT_FOUND_STATUS = 404
		private const val FORBIDDEN_STATUS = 403
		private const val SERVER_ERROR_STATUS = 500
		private const val SERVICE_UNAVAILABLE_STATUS = 503
	}
}
