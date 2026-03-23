package io.github.lishangbu.avalon.authorization.controller

import io.github.lishangbu.avalon.authorization.model.EmailCodeRequest
import io.github.lishangbu.avalon.authorization.model.SmsCodeRequest
import io.github.lishangbu.avalon.authorization.service.VerificationCodeService
import io.github.lishangbu.avalon.oauth2.common.core.AuthorizationGrantTypeSupport
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

/**
 * 验证码控制器
 *
 * 提供短信与邮箱验证码的发送接口
 *
 * @author lishangbu
 * @since 2026/3/13
 */
@RequestMapping("/auth")
@RestController
class VerificationCodeController(
    /** 验证码服务 */
    private val verificationCodeService: VerificationCodeService,
) {
    /**
     * 发送短信验证码
     *
     * @param request 短信验证码请求
     */
    @PostMapping("/sms/code")
    fun sendSmsCode(
        @RequestBody @Valid request: SmsCodeRequest,
    ) {
        verificationCodeService.generateCode(request.phone, AuthorizationGrantTypeSupport.SMS.value)
    }

    /**
     * 发送邮箱验证码
     *
     * @param request 邮箱验证码请求
     */
    @PostMapping("/email/code")
    fun sendEmailCode(
        @RequestBody @Valid request: EmailCodeRequest,
    ) {
        verificationCodeService.generateCode(
            request.email,
            AuthorizationGrantTypeSupport.EMAIL.value,
        )
    }
}
