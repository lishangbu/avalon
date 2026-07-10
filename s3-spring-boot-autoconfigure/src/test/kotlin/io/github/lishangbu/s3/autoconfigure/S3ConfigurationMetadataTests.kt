package io.github.lishangbu.s3.autoconfigure

import org.assertj.core.api.Assertions.assertThat
import tools.jackson.databind.JsonNode
import tools.jackson.databind.json.JsonMapper
import kotlin.test.Test

/**
 * 验证 Spring configuration metadata 随自动配置模块打包，保证 starter 使用者能获得 IDE 配置提示。
 */
class S3ConfigurationMetadataTests {
	private val objectMapper = JsonMapper.builder().build()

	@Test
	fun `configuration metadata is packaged with all s3 properties`() {
		val metadata = readMetadata()

		assertThat(metadata.namesAt("groups"))
			.containsExactlyInAnyOrder("s3", "s3.credentials", "s3.presign")
		assertThat(metadata.namesAt("properties"))
			.containsExactlyInAnyOrder(
				"s3.bucket",
				"s3.credentials.access-key",
				"s3.credentials.secret-key",
				"s3.credentials.session-token",
				"s3.enabled",
				"s3.endpoint",
				"s3.key-prefix",
				"s3.path-style-access-enabled",
				"s3.presign.default-ttl",
				"s3.region",
			)
	}

	@Test
	fun `configuration metadata keeps important property types`() {
		val propertiesByName = readMetadata()
			.path("properties")
			.associateBy { property -> property.path("name").asString() }

		assertThat(propertiesByName.typeOf("s3.enabled")).isEqualTo("java.lang.Boolean")
		assertThat(propertiesByName.typeOf("s3.path-style-access-enabled")).isEqualTo("java.lang.Boolean")
		assertThat(propertiesByName.typeOf("s3.endpoint")).isEqualTo("java.net.URI")
		assertThat(propertiesByName.typeOf("s3.presign.default-ttl")).isEqualTo("java.time.Duration")
	}

	/**
	 * 从测试运行时 classpath 读取实际打包资源，避免只验证源码目录里的 metadata 文件。
	 */
	private fun readMetadata(): JsonNode {
		val resource = checkNotNull(javaClass.classLoader.getResource(METADATA_RESOURCE)) {
			"$METADATA_RESOURCE 应打包进 s3-spring-boot-autoconfigure"
		}
		return resource.openStream().use(objectMapper::readTree)
	}

	/**
	 * 提取 metadata 分组或属性名称，保持断言聚焦在对外可见的配置键集合上。
	 */
	private fun JsonNode.namesAt(fieldName: String): Set<String> =
		path(fieldName)
			.values()
			.map { entry -> entry.path("name").asString() }
			.toSet()

	/**
	 * 按属性名读取 metadata 类型，用于保护关键配置项的绑定类型不被误改。
	 */
	private fun Map<String, JsonNode>.typeOf(propertyName: String): String =
		checkNotNull(this[propertyName]) {
			"$propertyName 应存在于 Spring configuration metadata"
		}.path("type").asString()

	private companion object {
		private const val METADATA_RESOURCE = "META-INF/spring-configuration-metadata.json"
	}
}
