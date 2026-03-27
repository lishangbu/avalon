package io.github.lishangbu.avalon.oauth2.authorizationserver.autoconfiguration

import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.source.ImmutableJWKSet
import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.proc.SecurityContext
import io.github.lishangbu.avalon.oauth2.common.properties.Oauth2Properties
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.mockito.MockedStatic
import org.mockito.Mockito
import org.springframework.core.io.AbstractResource
import org.springframework.core.io.DefaultResourceLoader
import org.springframework.core.io.Resource
import org.springframework.core.io.ResourceLoader
import org.springframework.test.util.ReflectionTestUtils
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.*

class JWKSourceAutoConfigurationTest {
    @TempDir
    lateinit var tempDir: Path

    @Test
    fun jwkSourceGeneratesKeysWhenNoLocationsProvided() {
        val properties = Oauth2Properties()
        val configuration = JWKSourceAutoConfiguration(properties, DefaultResourceLoader())

        configuration.afterPropertiesSet()
        val jwkSource: JWKSource<SecurityContext> = configuration.jwkSource()
        val secondCall: JWKSource<SecurityContext> = configuration.jwkSource()

        assertNotNull(jwkSource)
        assertNotNull(secondCall)
        val jwkSet = (jwkSource as ImmutableJWKSet<SecurityContext>).jwkSet
        assertEquals(1, jwkSet.keys.size)
    }

    @Test
    fun jwkSourceLoadsKeysFromConfiguredLocations() {
        val keyPair = generateKeyPair()
        val publicKeyFile = tempDir.resolve("public.pem")
        val privateKeyFile = tempDir.resolve("private.pem")
        writePem(publicKeyFile, "PUBLIC KEY", keyPair.public.encoded)
        writePem(privateKeyFile, "PRIVATE KEY", keyPair.private.encoded)

        val properties = Oauth2Properties()
        properties.jwtPublicKeyLocation = "file:$publicKeyFile"
        properties.jwtPrivateKeyLocation = "file:$privateKeyFile"

        val configuration = JWKSourceAutoConfiguration(properties, DefaultResourceLoader())
        configuration.afterPropertiesSet()

        val jwkSet = (configuration.jwkSource() as ImmutableJWKSet<SecurityContext>).jwkSet
        val rsaKey = jwkSet.keys.first() as RSAKey

        assertEquals((keyPair.public as RSAPublicKey).modulus, rsaKey.toRSAPublicKey().modulus)
        assertNotNull(rsaKey.toRSAPrivateKey())
    }

    @Test
    fun afterPropertiesSetHandlesMissingResourcesAndInvalidContent() {
        val properties = Oauth2Properties()
        properties.jwtPublicKeyLocation = "file:${tempDir.resolve("missing.pub")}"
        properties.jwtPrivateKeyLocation = "file:${tempDir.resolve("missing.key")}"

        val configuration = JWKSourceAutoConfiguration(properties, DefaultResourceLoader())
        configuration.afterPropertiesSet()

        val invalidPublic = tempDir.resolve("invalid.pub")
        val invalidPrivate = tempDir.resolve("invalid.key")
        Files.writeString(invalidPublic, "not-a-key", StandardCharsets.UTF_8)
        Files.writeString(invalidPrivate, "not-a-key", StandardCharsets.UTF_8)

        val invalidProps = Oauth2Properties()
        invalidProps.jwtPublicKeyLocation = "file:$invalidPublic"
        invalidProps.jwtPrivateKeyLocation = "file:$invalidPrivate"

        val invalidConfig = JWKSourceAutoConfiguration(invalidProps, DefaultResourceLoader())
        invalidConfig.afterPropertiesSet()
        assertNotNull(invalidConfig.jwkSource())
    }

    @Test
    fun afterPropertiesSetHandlesUnreadableResources() {
        val properties = Oauth2Properties()
        properties.jwtPublicKeyLocation = "unreadable:public"
        properties.jwtPrivateKeyLocation = "unreadable:private"

        val resourceLoader =
            object : ResourceLoader {
                override fun getResource(location: String): Resource =
                    object : AbstractResource() {
                        override fun getDescription(): String = "unreadable"

                        override fun exists(): Boolean = true

                        override fun isReadable(): Boolean = true

                        override fun getInputStream(): InputStream = throw IOException("boom")
                    }

                override fun getClassLoader(): ClassLoader = javaClass.classLoader
            }

        val configuration = JWKSourceAutoConfiguration(properties, resourceLoader)
        configuration.afterPropertiesSet()
    }

    @Test
    fun afterPropertiesSetWarnsWhenOnlyOneKeyProvided() {
        val keyPair = generateKeyPair()
        val publicKeyFile = tempDir.resolve("public-only.pem")
        writePem(publicKeyFile, "PUBLIC KEY", keyPair.public.encoded)

        val properties = Oauth2Properties()
        properties.jwtPublicKeyLocation = "file:$publicKeyFile"
        properties.jwtPrivateKeyLocation = " "

        val configuration = JWKSourceAutoConfiguration(properties, DefaultResourceLoader())
        configuration.afterPropertiesSet()
        assertNotNull(configuration.jwkSource())

        val privateOnly = Oauth2Properties()
        val privateKeyFile = tempDir.resolve("private-only.pem")
        writePem(privateKeyFile, "PRIVATE KEY", keyPair.private.encoded)
        privateOnly.jwtPublicKeyLocation = " "
        privateOnly.jwtPrivateKeyLocation = "file:$privateKeyFile"

        val privateConfig = JWKSourceAutoConfiguration(privateOnly, DefaultResourceLoader())
        privateConfig.afterPropertiesSet()
        assertNotNull(privateConfig.jwkSource())
    }

    @Test
    fun generateRsaKeyThrowsWhenAlgorithmMissing() {
        val properties = Oauth2Properties()
        val configuration = JWKSourceAutoConfiguration(properties, DefaultResourceLoader())

        val mocked: MockedStatic<KeyPairGenerator> =
            Mockito.mockStatic(KeyPairGenerator::class.java)
        mocked.use {
            it
                .`when`<KeyPairGenerator> { KeyPairGenerator.getInstance("RSA") }
                .thenThrow(NoSuchAlgorithmException("RSA"))

            val exception =
                assertThrows(IllegalStateException::class.java) {
                    ReflectionTestUtils.invokeMethod<Any>(configuration, "generateRsaKey")
                }
            assertNotNull(exception.message)
        }
    }

    @Test
    fun usesFallbackKidWhenThumbprintComputationFails() {
        val keyPair = generateKeyPair()
        val properties = Oauth2Properties()
        val configuration = JWKSourceAutoConfiguration(properties, DefaultResourceLoader())
        ReflectionTestUtils.setField(configuration, "publicKey", keyPair.public as RSAPublicKey)
        ReflectionTestUtils.setField(configuration, "privateKey", keyPair.private as RSAPrivateKey)

        val mocked: MockedStatic<MessageDigest> = Mockito.mockStatic(MessageDigest::class.java)
        mocked.use {
            it
                .`when`<MessageDigest> { MessageDigest.getInstance("SHA-256") }
                .thenThrow(NoSuchAlgorithmException("SHA-256"))

            val jwkSource: JWKSource<SecurityContext> = configuration.jwkSource()
            assertNotNull(jwkSource)
        }
    }

    companion object {
        private fun generateKeyPair(): KeyPair {
            val generator = KeyPairGenerator.getInstance("RSA")
            generator.initialize(1024)
            return generator.generateKeyPair()
        }

        private fun writePem(
            file: Path,
            type: String,
            encoded: ByteArray,
        ) {
            val content =
                "-----BEGIN $type-----\n" +
                    Base64.getEncoder().encodeToString(encoded) +
                    "\n-----END $type-----\n"
            Files.writeString(file, content, StandardCharsets.UTF_8)
        }
    }
}
