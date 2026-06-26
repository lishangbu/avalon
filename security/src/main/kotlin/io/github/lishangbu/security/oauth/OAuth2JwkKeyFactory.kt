package io.github.lishangbu.security.oauth

import com.nimbusds.jose.jwk.RSAKey
import java.security.KeyPair
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey
import java.util.UUID

/**
 * 生成授权服务器签名用 RSA JWK。
 *
 * 该工厂集中管理 key size 与 `kid` 生成规则，避免自动初始化和管理端轮换使用不同算法。
 */
class OAuth2JwkKeyFactory {
	fun generateRsaJwk(): RSAKey {
		val keyPair = generateRsaKey()
		return RSAKey.Builder(keyPair.public as RSAPublicKey)
			.privateKey(keyPair.private as RSAPrivateKey)
			.keyID(UUID.randomUUID().toString())
			.build()
	}

	private fun generateRsaKey(): KeyPair {
		val keyPairGenerator = KeyPairGenerator.getInstance("RSA")
		keyPairGenerator.initialize(2048)
		return keyPairGenerator.generateKeyPair()
	}
}
