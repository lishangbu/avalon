package io.github.lishangbu.config

import com.fasterxml.jackson.annotation.JsonProperty
import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.Schema
import org.springdoc.core.customizers.GlobalOpenApiCustomizer
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import java.util.Collections
import java.util.IdentityHashMap
import kotlin.reflect.KProperty1
import kotlin.reflect.full.memberProperties

/**
 * Restores response required-property metadata that springdoc cannot infer from Jimmer immutable interfaces.
 *
 * OpenAPI 3.1 already represents nullable Kotlin properties with a `null` schema type. Response properties that do
 * not allow `null` are therefore always emitted by the backend and must be marked as required for generated clients.
 * Request-only schemas are deliberately left unchanged because constructor defaults can make a non-null request
 * property optional on input.
 */
@Order(Ordered.LOWEST_PRECEDENCE)
class OpenApiResponseRequiredPropertiesCustomizer : GlobalOpenApiCustomizer {
	override fun customise(openApi: OpenAPI) {
		val componentSchemas = openApi.components?.schemas ?: return
		val responseSchemaNames = linkedSetOf<String>()
		val visitedSchemas = Collections.newSetFromMap(IdentityHashMap<Schema<*>, Boolean>())
		val schemaClasses = resolveSchemaClasses(componentSchemas.keys)

		openApi.paths
			?.values
			.orEmpty()
			.flatMap { pathItem -> pathItem.readOperations() }
			.flatMap { operation -> operation.responses?.values.orEmpty() }
			.flatMap { response -> response.content?.values.orEmpty() }
			.mapNotNull { mediaType -> mediaType.schema }
			.forEach { schema ->
				collectResponseSchemaNames(
					schema = schema,
					componentSchemas = componentSchemas,
					responseSchemaNames = responseSchemaNames,
					visitedSchemas = visitedSchemas,
				)
			}

		responseSchemaNames.forEach { schemaName ->
			val schema = componentSchemas[schemaName] ?: return@forEach
			val inferredRequired = requiredPropertyNames(schema, schemaClasses[schemaName])
			if (inferredRequired.isNotEmpty()) {
				schema.required = (schema.required.orEmpty() + inferredRequired).distinct()
			}
		}
	}

	private fun requiredPropertyNames(schema: Schema<*>, schemaClass: Class<*>?): Set<String> {
		val properties = schema.properties.orEmpty()
		if (schemaClass == null) {
			return properties
				.filterValues { property -> !property.allowsNull() }
				.keys
		}

		val modelProperties = runCatching {
			schemaClass.kotlin.memberProperties.associateBy(::jsonPropertyName)
		}.getOrDefault(emptyMap())
		return properties.keys
			.filterTo(linkedSetOf()) { propertyName ->
				modelProperties[propertyName]?.returnType?.isMarkedNullable == false
			}
	}

	private fun resolveSchemaClasses(schemaNames: Set<String>): Map<String, Class<*>> =
		buildMap {
			schemaNames.forEach { schemaName ->
				MODEL_PACKAGES.firstNotNullOfOrNull { packageName ->
					runCatching { Class.forName("$packageName.$schemaName") }.getOrNull()
				}?.let { schemaClass -> registerSchemaClass(schemaClass, this) }
			}
		}

	private fun registerSchemaClass(schemaClass: Class<*>, schemaClasses: MutableMap<String, Class<*>>) {
		val annotatedName = schemaClass.getAnnotation(io.swagger.v3.oas.annotations.media.Schema::class.java)
			?.name
			?.takeIf(String::isNotBlank)
		val schemaName = annotatedName ?: schemaClass.simpleName
		if (schemaClasses.putIfAbsent(schemaName, schemaClass) == null) {
			schemaClass.declaredClasses.forEach { nestedClass -> registerSchemaClass(nestedClass, schemaClasses) }
		}
	}

	private fun jsonPropertyName(property: KProperty1<out Any, *>): String =
		property.getter.annotations
			.filterIsInstance<JsonProperty>()
			.firstOrNull()
			?.value
			?.takeIf(String::isNotBlank)
			?: property.name

	private fun collectResponseSchemaNames(
		schema: Schema<*>,
		componentSchemas: Map<String, Schema<*>>,
		responseSchemaNames: MutableSet<String>,
		visitedSchemas: MutableSet<Schema<*>>,
	) {
		val referencedSchemaName = schema.`$ref`
			?.takeIf { reference -> reference.startsWith(COMPONENT_SCHEMA_PREFIX) }
			?.removePrefix(COMPONENT_SCHEMA_PREFIX)
		if (referencedSchemaName != null && responseSchemaNames.add(referencedSchemaName)) {
			componentSchemas[referencedSchemaName]?.let { referencedSchema ->
				collectResponseSchemaNames(referencedSchema, componentSchemas, responseSchemaNames, visitedSchemas)
			}
		}
		if (!visitedSchemas.add(schema)) {
			return
		}

		schema.items?.let { child ->
			collectResponseSchemaNames(child, componentSchemas, responseSchemaNames, visitedSchemas)
		}
		schema.properties.orEmpty().values.forEach { child ->
			collectResponseSchemaNames(child, componentSchemas, responseSchemaNames, visitedSchemas)
		}
		(schema.additionalProperties as? Schema<*>)?.let { child ->
			collectResponseSchemaNames(child, componentSchemas, responseSchemaNames, visitedSchemas)
		}
		(schema.allOf.orEmpty() + schema.anyOf.orEmpty() + schema.oneOf.orEmpty()).forEach { child ->
			collectResponseSchemaNames(child, componentSchemas, responseSchemaNames, visitedSchemas)
		}
	}

	private fun Schema<*>.allowsNull(): Boolean =
		nullable == true ||
			type == NULL_TYPE ||
			types?.contains(NULL_TYPE) == true ||
			(anyOf.orEmpty() + oneOf.orEmpty()).any { alternative ->
				alternative.type == NULL_TYPE || alternative.types?.contains(NULL_TYPE) == true
			}

	private companion object {
		private const val COMPONENT_SCHEMA_PREFIX = "#/components/schemas/"
		private const val NULL_TYPE = "null"
		private val MODEL_PACKAGES = listOf(
			"io.github.lishangbu.battlerules.dto",
			"io.github.lishangbu.common.web",
			"io.github.lishangbu.gamedata.dto",
			"io.github.lishangbu.scheduler",
			"io.github.lishangbu.system.dto",
		)
	}
}
