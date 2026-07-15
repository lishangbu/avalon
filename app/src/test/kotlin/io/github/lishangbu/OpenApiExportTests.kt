package io.github.lishangbu

import io.github.lishangbu.security.SecurityApiAccessPostgresTestContainer
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Tag
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.context.WebApplicationContext
import java.nio.file.Files
import java.nio.file.Path

@Tag("integration")
@SpringBootTest(classes = [BackendApplication::class])
@ContextConfiguration(initializers = [SecurityApiAccessPostgresTestContainer::class])
class OpenApiExportTests(@Autowired private val context: WebApplicationContext) {
	@Test
	fun `exports authoritative openapi document`() {
		val response = MockMvcBuilders.webAppContextSetup(context).build()
			.perform(get("/v3/api-docs"))
			.andReturn().response
		check(response.status == 200)
		val output = Path.of("build/generated/openapi.json")
		Files.createDirectories(output.parent)
		Files.writeString(output, response.contentAsString)
	}
}
