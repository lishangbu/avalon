package io.github.lishangbu.avalon.oauth2.authorizationserver.autoconfiguration

import com.nimbusds.jose.JOSEException
import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.proc.SecurityContext
import io.github.lishangbu.avalon.oauth2.common.properties.Oauth2Properties
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.InitializingBean
import org.springframework.boot.autoconfigure.AutoConfiguration
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.context.annotation.Bean
import org.springframework.core.io.Resource
import org.springframework.core.io.ResourceLoader
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.security.KeyFactory
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.NoSuchAlgorithmException
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.security.spec.InvalidKeySpecException
import java.security.spec.PKCS8EncodedKeySpec
import java.security.spec.X509EncodedKeySpec
import java.util.*

/**
 * JWKSource 自动装配
 *
 * 负责提供用于签名的 JWKSource 实例，支持从配置加载公/私钥；若未配置则回退为运行时生成的密钥对
 *
 * 行为要点：
 * - 密钥在首次使用时懒初始化，线程安全
 * - 优先使用公钥 thumbprint 作为 kid，保证 kid 稳定可复现
 * - Spring Authorization Server 会自动从 JWKSource 提取公钥信息供 /.well-known/jwks.json 端点使用
 * - 支持从 classpath 或文件系统加载 PEM 格式的公私钥
 * - 加载失败时自动回退为随机生成的密钥对，并记录警告
 *
 * @author lishangbu
 * @since 2025/8/17
 */
@AutoConfiguration
class JWKSourceAutoConfiguration(
    private val oauth2Properties: Oauth2Properties,
    private val resourceLoader: ResourceLoader,
) : InitializingBean {
    private var publicKey: RSAPublicKey? = null

    private var privateKey: RSAPrivateKey? = null

    /**
     * 缓存的 JWKSet，包含私钥，用于 JWT 签名
     *
     * 使用 volatile 保证多线程环境下的可见性，配合 synchronized 方法实现线程安全的懒加载
     */
    @Volatile
    private var jwkSet: JWKSet? = null

    /**
     * 返回用于签名的 JWKSource
     *
     * 方法保证密钥已初始化（从配置加载或运行时生成），并返回一个不可变的 JWKSet 供框架选择签名密钥 Spring Authorization Server 会自动处理
     * /.well-known/jwks.json 端点，从此 JWKSource 提取公钥信息
     *
     * @return 包含私钥的不可变 JWKSource，用于 Authorization Server 的 JWT 签名
     */
    @Bean
    @ConditionalOnMissingBean
    fun jwkSource(): JWKSource<SecurityContext> {
        ensureKeysInitialized()
        return ImmutableJWKSet(requireNotNull(jwkSet) { "jwkSet cannot be null" })
    }

    /**
     * 确保密钥与 JWKSet 已经初始化（线程安全的懒初始化）
     *
     * 该方法执行以下操作：
     * - 检查是否已初始化，避免重复初始化
     * - 若公私钥为空，使用随机生成的密钥对并记录警告
     * - 计算 kid（优先使用公钥 thumbprint，失败时使用随机 UUID）
     * - 构建包含私钥的 JWKSet（框架会自动过滤私钥信息对外暴露）
     */
    @Synchronized
    private fun ensureKeysInitialized() {
        if (jwkSet != null) {
            return
        }

        // 如果没有通过配置加载到公私钥，则生成随机密钥对
        if (publicKey == null || privateKey == null) {
            log.warn("未配置公钥或私钥，使用随机生成的密钥对，重启后之前签发的 token 将无法解析")
            val keyPair = generateRsaKey()
            publicKey = keyPair.public as RSAPublicKey
            privateKey = keyPair.private as RSAPrivateKey
        }

        val rsaPublicKey = requireNotNull(publicKey) { "publicKey cannot be null" }
        val rsaPrivateKey = requireNotNull(privateKey) { "privateKey cannot be null" }

        // 计算 kid：优先使用公钥 thumbprint，计算失败时回退为公钥 modulus（避免依赖 MessageDigest）
        val kid =
            try {
                val computed =
                    RSAKey
                        .Builder(rsaPublicKey)
                        .build()
                        .computeThumbprint()
                        .toString()
                log.debug("使用公钥 thumbprint 作为 kid: {}", computed)
                computed
            } catch (e: JOSEException) {
                val fallback = rsaPublicKey.modulus.toString(16)
                log.warn("计算公钥 thumbprint 失败，回退为公钥 modulus: {}", fallback, e)
                fallback
            }

        // 使用默认算法 RS256，并标记为签名用途
        val alg = JWSAlgorithm.RS256

        // 构建 JWK（包含私钥）
        val rsaKey =
            RSAKey
                .Builder(rsaPublicKey)
                .privateKey(rsaPrivateKey)
                .keyID(kid)
                .algorithm(alg)
                .build()

        this.jwkSet = JWKSet(rsaKey)

        log.info("JWKSet 初始化完成，kid: {}, 算法: {}", kid, alg)
    }

    /**
     * 生成用于签名的 RSA 密钥对
     *
     * 使用 2048 位 RSA 算法生成密钥对，适用于 JWT 签名场景
     *
     * @return 生成的 RSA KeyPair，公钥和私钥均为 RSA 实现
     * @throws IllegalStateException 当密钥生成失败时抛出
     */
    private fun generateRsaKey(): KeyPair =
        try {
            val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
            keyPairGenerator.initialize(2048)
            val generated = keyPairGenerator.generateKeyPair()
            log.debug("成功生成 2048 位 RSA 密钥对")
            generated
        } catch (ex: Exception) {
            log.error("生成 RSA 密钥对失败", ex)
            throw IllegalStateException("无法生成 RSA 密钥对", ex)
        }

    /**
     * 初始化时尝试从配置的资源位置加载 RSA 公钥和私钥
     *
     * 优先使用 oauth2Properties 中的 jwtPublicKeyLocation 和 jwtPrivateKeyLocation 指定的资源
     * 若两者均成功加载则使用加载的密钥；若任一密钥缺失或解析失败则回退为随机生成的密钥对，并记录警告，注意重启后之前签发的 token 将不可解析
     *
     * 支持的资源位置格式：
     * - classpath: classpath:rsa/public.key
     * - 文件系统: file:/path/to/public.key
     * - 相对路径: rsa/public.key（相对于 classpath）
     */
    override fun afterPropertiesSet() {
        val jwtPublicKeyLocation = oauth2Properties.jwtPublicKeyLocation?.takeIf { it.isNotBlank() }
        val jwtPrivateKeyLocation = oauth2Properties.jwtPrivateKeyLocation?.takeIf { it.isNotBlank() }
        // 密钥缺失或解析失败，标记为未加载状态，将在首次使用时生成随机密钥对
        if (jwtPublicKeyLocation == null && jwtPrivateKeyLocation == null) {
            log.warn("未配置公钥和私钥路径，将在首次使用时生成随机密钥对")
            return
        }

        var loadedPublic: RSAPublicKey? = null
        var loadedPrivate: RSAPrivateKey? = null

        // 尝试加载公钥
        if (jwtPublicKeyLocation != null) {
            try {
                val resource: Resource = resourceLoader.getResource(jwtPublicKeyLocation)
                if (resource.exists() && resource.isReadable) {
                    resource.inputStream.use { inputStream: InputStream ->
                        val content = String(inputStream.readAllBytes(), StandardCharsets.UTF_8)
                        loadedPublic = loadPublicKey(content)
                        log.debug("从 [{}] 成功加载公钥", jwtPublicKeyLocation)
                    }
                } else {
                    log.error("公钥资源不存在或不可读: {}", jwtPublicKeyLocation)
                }
            } catch (e: IOException) {
                log.error("公钥读取失败，无法从 [{}] 检索到有效公钥: {}", jwtPublicKeyLocation, e.message)
            } catch (e: NoSuchAlgorithmException) {
                log.error("公钥解析失败，无法从 [{}] 解析到有效公钥: {}", jwtPublicKeyLocation, e.message)
            } catch (e: InvalidKeySpecException) {
                log.error("公钥解析失败，无法从 [{}] 解析到有效公钥: {}", jwtPublicKeyLocation, e.message)
            } catch (e: IllegalArgumentException) {
                log.error("公钥解析失败，无法从 [{}] 解析到有效公钥: {}", jwtPublicKeyLocation, e.message)
            }
        } else {
            log.warn("公钥或私钥加载不完整，将在首次使用时生成随机密钥对")
        }

        // 尝试加载私钥
        if (jwtPrivateKeyLocation != null) {
            try {
                val resource = resourceLoader.getResource(jwtPrivateKeyLocation)
                if (resource.exists() && resource.isReadable) {
                    resource.inputStream.use { inputStream: InputStream ->
                        val content = String(inputStream.readAllBytes(), StandardCharsets.UTF_8)
                        loadedPrivate = loadPrivateKey(content)
                        log.debug("从 [{}] 成功加载私钥", jwtPrivateKeyLocation)
                    }
                } else {
                    log.error("私钥资源不存在或不可读: {}", jwtPrivateKeyLocation)
                }
            } catch (e: IOException) {
                log.error("私钥读取失败，无法从 [{}] 检索到有效私钥: {}", jwtPrivateKeyLocation, e.message)
            } catch (e: NoSuchAlgorithmException) {
                log.error("私钥解析失败，无法从 [{}] 解析到有效私钥: {}", jwtPrivateKeyLocation, e.message)
            } catch (e: InvalidKeySpecException) {
                log.error("私钥解析失败，无法从 [{}] 解析到有效私钥: {}", jwtPrivateKeyLocation, e.message)
            } catch (e: IllegalArgumentException) {
                log.error("私钥解析失败，无法从 [{}] 解析到有效私钥: {}", jwtPrivateKeyLocation, e.message)
            }
        } else {
            log.warn("私钥加载不完整，将在首次使用时生成随机密钥对")
        }

        // 若公私钥均已加载则直接使用
        if (loadedPublic != null && loadedPrivate != null) {
            this.publicKey = loadedPublic
            this.privateKey = loadedPrivate
            log.debug("成功从配置加载公私钥对")
        }
    }

    companion object {
        private val log: Logger = LoggerFactory.getLogger(JWKSourceAutoConfiguration::class.java)

        /**
         * 从 PEM 格式的公钥字符串加载 RSA 公钥实例
         *
         * 支持标准的 PEM 格式，自动移除头尾行和空白字符
         *
         * @param publicKeyContent 公钥的 PEM 文本，允许包含头尾行和换行
         * @return 解析得到的 [RSAPublicKey]
         * @throws NoSuchAlgorithmException 当 RSA 算法不可用时抛出
         * @throws InvalidKeySpecException 当密钥格式不符合 X.509 编码时抛出
         */
        @Throws(NoSuchAlgorithmException::class, InvalidKeySpecException::class)
        private fun loadPublicKey(publicKeyContent: String): RSAPublicKey {
            val normalized =
                publicKeyContent
                    .replace("-----BEGIN PUBLIC KEY-----", "")
                    .replace("-----END PUBLIC KEY-----", "")
                    .replace("\\s".toRegex(), "")
            val encoded = Base64.getDecoder().decode(normalized)
            val keyFactory = KeyFactory.getInstance("RSA")
            return keyFactory.generatePublic(X509EncodedKeySpec(encoded)) as RSAPublicKey
        }

        /**
         * 从 PEM 格式的私钥字符串加载 RSA 私钥实例
         *
         * 支持标准的 PEM 格式（PKCS#8），自动移除头尾行和空白字符
         *
         * @param privateKeyContent 私钥的 PEM 文本，允许包含头尾行和换行
         * @return 解析得到的 [RSAPrivateKey]
         * @throws NoSuchAlgorithmException 当 RSA 算法不可用时抛出
         * @throws InvalidKeySpecException 当密钥格式不符合 PKCS#8 编码时抛出
         */
        @Throws(NoSuchAlgorithmException::class, InvalidKeySpecException::class)
        private fun loadPrivateKey(privateKeyContent: String): RSAPrivateKey {
            val normalized =
                privateKeyContent
                    .replace("-----BEGIN PRIVATE KEY-----", "")
                    .replace("-----END PRIVATE KEY-----", "")
                    .replace("\\s".toRegex(), "")
            val encoded = Base64.getDecoder().decode(normalized)
            val keyFactory = KeyFactory.getInstance("RSA")
            return keyFactory.generatePrivate(PKCS8EncodedKeySpec(encoded)) as RSAPrivateKey
        }
    }
}
