package io.github.lishangbu.common.persistence.jimmer

import me.ahoo.cosid.IdGenerator
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.context.ApplicationContext

@SpringBootTest(
	classes = [CosIdJimmerAutoConfigurationTestApplication::class],
	properties = [
		"cosid.machine.enabled=true",
		"cosid.machine.distributor.manual.machine-id=0",
		"cosid.snowflake.enabled=true",
		"cosid.snowflake.zone-id=UTC",
	],
)
/**
 * 验证 common-persistence 只桥接官方 CosId starter 和 Jimmer ID 生成器。
 */
class CosIdJimmerAutoConfigurationTests(
	@Autowired private val applicationContext: ApplicationContext,
	@Autowired private val jimmerGenerator: CosIdLongUserIdGenerator,
) {
	@Test
	fun `jimmer long id generator uses official cosid starter bean`() {
		val officialGenerator = applicationContext.getBean("__share__SnowflakeId", IdGenerator::class.java)

		val firstId = jimmerGenerator.generate(String::class.java)
		val secondId = jimmerGenerator.generate(String::class.java)

		assertThat(officialGenerator).isNotNull
		assertThat(firstId).isPositive()
		assertThat(secondId).isPositive()
		assertThat(firstId).isNotEqualTo(secondId)
	}

	@Test
	fun `common persistence does not expose backend branded cosid properties`() {
		val backendCosIdProperties = runCatching {
			Class.forName("io.github.lishangbu.common.persistence.cosid.BackendCosIdProperties")
		}

		assertThat(backendCosIdProperties.isFailure).isTrue()
	}
}
