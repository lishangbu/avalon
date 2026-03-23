package io.github.lishangbu.avalon.authorization.service.impl

internal fun String?.toCommaDelimitedSet(): LinkedHashSet<String> {
    if (this == null) {
        return linkedSetOf()
    }

    val values = linkedSetOf<String>()
    var startIndex = 0

    while (true) {
        val delimiterIndex = indexOf(',', startIndex)
        if (delimiterIndex < 0) {
            values.add(substring(startIndex))
            return values
        }
        values.add(substring(startIndex, delimiterIndex))
        startIndex = delimiterIndex + 1
    }
}

internal fun Iterable<String>.joinToCommaDelimitedString(): String = joinToString(",")
