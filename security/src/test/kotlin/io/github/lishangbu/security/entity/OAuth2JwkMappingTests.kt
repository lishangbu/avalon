package io.github.lishangbu.security.entity

import io.github.lishangbu.common.persistence.jimmer.CosIdLongUserIdGenerator
import org.assertj.core.api.Assertions.assertThat
import org.babyfish.jimmer.sql.GeneratedValue
import org.junit.jupiter.api.Test
import kotlin.reflect.full.findAnnotation

/**
 * 验证授权服务实体的 Long 主键继续使用公共 CosId Jimmer 生成器。
 */
class OAuth2JwkMappingTests {
	@Test
	fun `jwk id uses shared cosid jimmer generator`() {
		val generatedValue = OAuth2Jwk::id.findAnnotation<GeneratedValue>()

		assertThat(OAuth2Jwk::id.returnType.classifier).isEqualTo(Long::class)
		assertThat(generatedValue).isNotNull
		assertThat(generatedValue!!.generatorType).isEqualTo(CosIdLongUserIdGenerator::class)
	}

	@Test
	fun `oauth client id uses shared cosid jimmer generator`() {
		val generatedValue = OAuth2Client::id.findAnnotation<GeneratedValue>()

		assertThat(OAuth2Client::id.returnType.classifier).isEqualTo(Long::class)
		assertThat(generatedValue).isNotNull
		assertThat(generatedValue!!.generatorType).isEqualTo(CosIdLongUserIdGenerator::class)
	}
}
