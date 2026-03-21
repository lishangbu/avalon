package io.github.lishangbu.avalon.oauth2.common.constant

/**
 * 安全相关 Bean 顺序配置常量 定义授权服务器和资源服务器在 Spring 安全链中的默认顺序与 bean 名称
 *
 * @author lishangbu
 * @since 2025/8/17
 */
object SecurityBeanDefinitionConstants {
    const val AUTHORIZATION_SERVER_SECURITY_FILTER_CHAIN_BEAN_ORDER = 1
    const val RESOURCE_SERVER_SECURITY_FILTER_CHAIN_BEAN_ORDER =
        AUTHORIZATION_SERVER_SECURITY_FILTER_CHAIN_BEAN_ORDER + 1
    const val AUTHORIZATION_SERVER_SECURITY_FILTER_CHAIN_BEAN_NAME =
        "authorizationServerSecurityFilterChain"
    const val RESOURCE_SERVER_SECURITY_FILTER_CHAIN_BEAN_NAME = "resourceServerSecurityFilterChain"
    const val AUTHORIZATION_REDIS_TEMPLATE = "authorizationRedisTemplate"
}
