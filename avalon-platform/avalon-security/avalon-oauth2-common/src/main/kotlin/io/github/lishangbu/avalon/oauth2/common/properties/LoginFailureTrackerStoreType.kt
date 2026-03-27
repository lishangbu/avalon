package io.github.lishangbu.avalon.oauth2.common.properties

/**
 * Supported login failure tracker stores.
 */
enum class LoginFailureTrackerStoreType {
    MEMORY,
    REDIS,
    JDBC,
}
