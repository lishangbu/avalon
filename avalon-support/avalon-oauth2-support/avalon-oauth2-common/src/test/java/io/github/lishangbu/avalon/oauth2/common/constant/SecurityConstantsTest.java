package io.github.lishangbu.avalon.oauth2.common.constant;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

class SecurityConstantsTest {

    @Test
    void beanDefinitionConstantsHaveExpectedOrdersAndNames() {
        assertEquals(1, SecurityBeanDefinitionConstants.AUTHORIZATION_SERVER_SECURITY_FILTER_CHAIN_BEAN_ORDER);
        assertEquals(
                2, SecurityBeanDefinitionConstants.RESOURCE_SERVER_SECURITY_FILTER_CHAIN_BEAN_ORDER);
        assertEquals(
                "authorizationServerSecurityFilterChain",
                SecurityBeanDefinitionConstants.AUTHORIZATION_SERVER_SECURITY_FILTER_CHAIN_BEAN_NAME);
        assertEquals(
                "resourceServerSecurityFilterChain",
                SecurityBeanDefinitionConstants.RESOURCE_SERVER_SECURITY_FILTER_CHAIN_BEAN_NAME);
        assertEquals("authorizationRedisTemplate", SecurityBeanDefinitionConstants.AUTHORIZATION_REDIS_TEMPLATE);
    }

    @Test
    void securityConstantsExposeKeys() {
        assertEquals("login-type", SecurityConstants.LOGIN_TYPE);
        assertEquals("authorities", SecurityConstants.AUTHORITIES_KEY);
        assertEquals("token-unique-id", SecurityConstants.TOKEN_UNIQUE_ID);
    }
}
