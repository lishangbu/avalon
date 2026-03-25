package io.github.lishangbu.avalon.authorization.controller

import io.github.lishangbu.avalon.authorization.model.EmailCodeRequest
import io.github.lishangbu.avalon.authorization.model.SmsCodeRequest
import io.github.lishangbu.avalon.authorization.service.VerificationCodeService
import io.github.lishangbu.avalon.oauth2.common.core.AuthorizationGrantTypeSupport
import org.junit.jupiter.api.Test
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

class VerificationCodeControllerTest {
    private val verificationCodeService = mock(VerificationCodeService::class.java)
    private val controller = VerificationCodeController(verificationCodeService)

    @Test
    fun sendsSmsAndEmailCodesWithExpectedGrantTypes() {
        controller.sendSmsCode(SmsCodeRequest("13800138000"))
        controller.sendEmailCode(EmailCodeRequest("user@example.com"))

        verify(verificationCodeService).generateCode("13800138000", AuthorizationGrantTypeSupport.SMS.value)
        verify(verificationCodeService).generateCode("user@example.com", AuthorizationGrantTypeSupport.EMAIL.value)
    }
}
