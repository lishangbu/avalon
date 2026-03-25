package io.github.lishangbu.avalon.authorization.service.impl

import io.github.lishangbu.avalon.oauth2.authorizationserver.exception.InvalidCaptchaException
import io.github.lishangbu.avalon.oauth2.common.core.AuthorizationGrantTypeSupport
import io.github.lishangbu.avalon.oauth2.common.properties.Oauth2Properties
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.data.redis.core.StringRedisTemplate
import org.springframework.data.redis.core.ValueOperations
import java.time.Duration

class AbstractVerificationCodeServiceTest {
    private val stringRedisTemplate = mock(StringRedisTemplate::class.java)

    @Suppress("UNCHECKED_CAST")
    private val valueOperations = mock(ValueOperations::class.java) as ValueOperations<String, String>

    private val properties =
        Oauth2Properties().apply {
            emailCodeLength = 4
            smsCodeLength = 8
        }

    private val service = TestVerificationCodeService(stringRedisTemplate, properties)

    init {
        `when`(stringRedisTemplate.opsForValue()).thenReturn(valueOperations)
    }

    @Test
    fun generateCodeNormalizesTargetStoresValueAndDeliversCode() {
        `when`(valueOperations.setIfAbsent(any(), any(), any())).thenReturn(true)

        val code = service.generateCode(" User@Example.com ", " EMAIL ")

        assertEquals(6, code.length)
        assertEquals(Triple("email", "user@example.com", code), service.delivery)
        verify(valueOperations).setIfAbsent(
            "oauth2:verification:rate:email:user@example.com",
            "1",
            Duration.ofSeconds(60),
        )
        verify(valueOperations).set(
            "oauth2:verification:code:email:user@example.com",
            code,
            Duration.ofMinutes(5),
        )
    }

    @Test
    fun generateCodeRejectsFrequentRequests() {
        `when`(valueOperations.setIfAbsent(any(), any(), any())).thenReturn(false)

        val exception =
            assertThrows(InvalidCaptchaException::class.java) {
                service.generateCode("13800138000", AuthorizationGrantTypeSupport.SMS.value)
            }

        assertEquals("验证码发送过于频繁，请稍后再试", exception.message)
        verifyNoInteractions(service.deliveryTarget)
    }

    @Test
    fun verifyCodeDeletesCachedValueWhenMatched() {
        `when`(valueOperations.get("oauth2:verification:code:email:user@example.com")).thenReturn("123456")

        service.verifyCode(" User@Example.com ", " 123456 ", " EMAIL ")

        verify(stringRedisTemplate).delete("oauth2:verification:code:email:user@example.com")
    }

    @Test
    fun verifyCodeRejectsMissingOrMismatchedCode() {
        `when`(valueOperations.get("oauth2:verification:code:sms:13800138000")).thenReturn(null).thenReturn("654321")

        val missing =
            assertThrows(InvalidCaptchaException::class.java) {
                service.verifyCode("13800138000", "123456", AuthorizationGrantTypeSupport.SMS.value)
            }
        assertEquals("验证码已过期或不存在", missing.message)

        val mismatch =
            assertThrows(InvalidCaptchaException::class.java) {
                service.verifyCode("13800138000", "123456", AuthorizationGrantTypeSupport.SMS.value)
            }
        assertEquals("验证码错误", mismatch.message)
    }

    @Test
    fun generateAndVerifyRejectBlankArguments() {
        assertEquals(
            "验证码类型不能为空",
            assertThrows(InvalidCaptchaException::class.java) { service.generateCode("13800138000", " ") }.message,
        )
        assertEquals(
            "验证码接收目标不能为空",
            assertThrows(InvalidCaptchaException::class.java) { service.generateCode(" ", AuthorizationGrantTypeSupport.SMS.value) }.message,
        )
        assertEquals(
            "验证码不能为空",
            assertThrows(InvalidCaptchaException::class.java) {
                service.verifyCode("13800138000", " ", AuthorizationGrantTypeSupport.SMS.value)
            }.message,
        )
    }

    private class TestVerificationCodeService(
        stringRedisTemplate: StringRedisTemplate,
        oauth2Properties: Oauth2Properties,
    ) : AbstractVerificationCodeService(stringRedisTemplate, oauth2Properties) {
        var delivery: Triple<String, String, String>? = null
        val deliveryTarget = mock(Any::class.java)

        override fun deliverCode(
            type: String,
            target: String,
            code: String,
        ) {
            delivery = Triple(type, target, code)
            deliveryTarget.toString()
        }
    }
}
