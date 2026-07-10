package io.github.lishangbu.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.media.Schema
import org.springdoc.core.customizers.GlobalOpenApiCustomizer
import org.springframework.core.Ordered
import org.springframework.core.annotation.Order
import java.util.Collections
import java.util.IdentityHashMap

/**
 * Documents database identifiers as JSON strings in requests, responses, and path parameters.
 *
 * Runtime binders already accept decimal strings for `Long` values. Publishing them as numbers would invite browser
 * clients to round Snowflake IDs beyond JavaScript's safe-integer range before the request reaches the backend.
 * Ordinary long-valued measurements and durations remain numeric because only identifier-shaped names are changed.
 */
@Order(Ordered.LOWEST_PRECEDENCE - 1)
class OpenApiIdentifierSchemaCustomizer : GlobalOpenApiCustomizer {
	override fun customise(openApi: OpenAPI) {
		val visitedSchemas = Collections.newSetFromMap(IdentityHashMap<Schema<*>, Boolean>())
		openApi.components?.schemas?.values.orEmpty().forEach { schema ->
			normalizeIdentifierProperties(schema, visitedSchemas)
		}
		openApi.components?.parameters?.values.orEmpty().forEach(::normalizeIdentifierParameter)
		openApi.paths?.values.orEmpty().forEach { pathItem ->
			pathItem.parameters.orEmpty().forEach(::normalizeIdentifierParameter)
			pathItem.readOperations().forEach { operation ->
				operation.parameters.orEmpty().forEach(::normalizeIdentifierParameter)
			}
		}
	}

	private fun normalizeIdentifierProperties(schema: Schema<*>, visitedSchemas: MutableSet<Schema<*>>) {
		if (!visitedSchemas.add(schema)) {
			return
		}

		schema.properties.orEmpty().forEach { (propertyName, propertySchema) ->
			if (propertyName.isIdentifierName()) {
				propertySchema.normalizeIdentifierType()
			}
			normalizeIdentifierProperties(propertySchema, visitedSchemas)
		}
		schema.items?.let { items -> normalizeIdentifierProperties(items, visitedSchemas) }
		(schema.additionalProperties as? Schema<*>)?.let { additionalProperties ->
			normalizeIdentifierProperties(additionalProperties, visitedSchemas)
		}
		(schema.allOf.orEmpty() + schema.anyOf.orEmpty() + schema.oneOf.orEmpty()).forEach { child ->
			normalizeIdentifierProperties(child, visitedSchemas)
		}
	}

	private fun normalizeIdentifierParameter(parameter: io.swagger.v3.oas.models.parameters.Parameter) {
		if (parameter.name?.isIdentifierName() == true) {
			parameter.schema?.normalizeIdentifierType()
		}
	}

	private fun Schema<*>.normalizeIdentifierType() {
		if (isArraySchema()) {
			items?.normalizeIdentifierScalarType()
		} else {
			normalizeIdentifierScalarType()
		}
	}

	private fun Schema<*>.normalizeIdentifierScalarType() {
		val nullableIdentifier = allowsNull()
		type = STRING_TYPE
		format = null
		types = if (nullableIdentifier) {
			linkedSetOf(STRING_TYPE, NULL_TYPE)
		} else {
			linkedSetOf(STRING_TYPE)
		}
		anyOf = null
		oneOf = null
	}

	private fun Schema<*>.isArraySchema(): Boolean =
		type == ARRAY_TYPE || types?.contains(ARRAY_TYPE) == true || items != null

	private fun Schema<*>.allowsNull(): Boolean =
		nullable == true ||
			type == NULL_TYPE ||
			types?.contains(NULL_TYPE) == true ||
			(anyOf.orEmpty() + oneOf.orEmpty()).any { alternative ->
				alternative.type == NULL_TYPE || alternative.types?.contains(NULL_TYPE) == true
			}

	private fun String.isIdentifierName(): Boolean =
		this == "id" || endsWith("_id") || endsWith("_ids") || endsWith("Id") || endsWith("Ids")

	private companion object {
		private const val ARRAY_TYPE = "array"
		private const val NULL_TYPE = "null"
		private const val STRING_TYPE = "string"
	}
}
