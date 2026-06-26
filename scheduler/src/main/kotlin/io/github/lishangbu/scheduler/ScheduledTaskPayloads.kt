package io.github.lishangbu.scheduler

import com.fasterxml.jackson.core.type.TypeReference
import com.fasterxml.jackson.databind.ObjectMapper

internal val SCHEDULED_TASK_PAYLOAD_TYPE = object : TypeReference<Map<String, Any?>>() {}

internal fun ObjectMapper.writePayload(payload: Map<String, Any?>): String =
	writeValueAsString(payload)

internal fun ObjectMapper.readPayload(payloadJson: String?): Map<String, Any?> =
	payloadJson
		?.takeIf { it.isNotBlank() }
		?.let { readValue(it, SCHEDULED_TASK_PAYLOAD_TYPE) }
		.orEmpty()
