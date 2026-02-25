package io.github.lishangbu.avalon.oauth2.common.constant;

/// 安全相关 Bean 顺序配置常量
///
/// 定义授权服务器和资源服务器在 Spring 安全链中的默认顺序与 bean 名称
///
/// @author lishangbu
/// @since 2025/8/17
public abstract class SecurityBeanDefinitionConstants {

    public static final int AUTHORIZATION_SERVER_SECURITY_FILTER_CHAIN_BEAN_ORDER = 1;

    public static final int RESOURCE_SERVER_SECURITY_FILTER_CHAIN_BEAN_ORDER =
            AUTHORIZATION_SERVER_SECURITY_FILTER_CHAIN_BEAN_ORDER + 1;
    public static final String AUTHORIZATION_SERVER_SECURITY_FILTER_CHAIN_BEAN_NAME =
            "authorizationServerSecurityFilterChain";
    public static final String RESOURCE_SERVER_SECURITY_FILTER_CHAIN_BEAN_NAME =
            "resourceServerSecurityFilterChain";

    public static final String AUTHORIZATION_REDIS_TEMPLATE = "authorizationRedisTemplate";
}
