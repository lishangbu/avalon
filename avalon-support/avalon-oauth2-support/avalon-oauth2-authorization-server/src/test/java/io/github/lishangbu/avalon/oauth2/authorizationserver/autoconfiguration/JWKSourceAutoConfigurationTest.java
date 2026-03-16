package io.github.lishangbu.avalon.oauth2.authorizationserver.autoconfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import io.github.lishangbu.avalon.oauth2.common.properties.Oauth2Properties;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.springframework.core.io.AbstractResource;
import org.springframework.core.io.DefaultResourceLoader;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.test.util.ReflectionTestUtils;

class JWKSourceAutoConfigurationTest {

    @TempDir Path tempDir;

    @Test
    void jwkSourceGeneratesKeysWhenNoLocationsProvided() {
        Oauth2Properties properties = new Oauth2Properties();
        JWKSourceAutoConfiguration configuration =
                new JWKSourceAutoConfiguration(properties, new DefaultResourceLoader());

        configuration.afterPropertiesSet();
        JWKSource<SecurityContext> jwkSource = configuration.jwkSource();
        JWKSource<SecurityContext> secondCall = configuration.jwkSource();

        assertNotNull(jwkSource);
        assertNotNull(secondCall);
        JWKSet jwkSet = ((ImmutableJWKSet<SecurityContext>) jwkSource).getJWKSet();
        assertEquals(1, jwkSet.getKeys().size());
    }

    @Test
    void jwkSourceLoadsKeysFromConfiguredLocations() throws Exception {
        KeyPair keyPair = generateKeyPair();
        Path publicKeyFile = tempDir.resolve("public.pem");
        Path privateKeyFile = tempDir.resolve("private.pem");
        writePem(publicKeyFile, "PUBLIC KEY", keyPair.getPublic().getEncoded());
        writePem(privateKeyFile, "PRIVATE KEY", keyPair.getPrivate().getEncoded());

        Oauth2Properties properties = new Oauth2Properties();
        properties.setJwtPublicKeyLocation("file:" + publicKeyFile.toString());
        properties.setJwtPrivateKeyLocation("file:" + privateKeyFile.toString());

        JWKSourceAutoConfiguration configuration =
                new JWKSourceAutoConfiguration(properties, new DefaultResourceLoader());
        configuration.afterPropertiesSet();

        JWKSet jwkSet = ((ImmutableJWKSet<SecurityContext>) configuration.jwkSource()).getJWKSet();
        RSAKey rsaKey = (RSAKey) jwkSet.getKeys().getFirst();

        assertEquals(
                ((RSAPublicKey) keyPair.getPublic()).getModulus(),
                rsaKey.toRSAPublicKey().getModulus());
        assertNotNull(rsaKey.toRSAPrivateKey());
    }

    @Test
    void afterPropertiesSetHandlesMissingResourcesAndInvalidContent() throws Exception {
        Oauth2Properties properties = new Oauth2Properties();
        properties.setJwtPublicKeyLocation("file:" + tempDir.resolve("missing.pub"));
        properties.setJwtPrivateKeyLocation("file:" + tempDir.resolve("missing.key"));

        JWKSourceAutoConfiguration configuration =
                new JWKSourceAutoConfiguration(properties, new DefaultResourceLoader());
        configuration.afterPropertiesSet();

        Path invalidPublic = tempDir.resolve("invalid.pub");
        Path invalidPrivate = tempDir.resolve("invalid.key");
        Files.writeString(invalidPublic, "not-a-key", StandardCharsets.UTF_8);
        Files.writeString(invalidPrivate, "not-a-key", StandardCharsets.UTF_8);

        Oauth2Properties invalidProps = new Oauth2Properties();
        invalidProps.setJwtPublicKeyLocation("file:" + invalidPublic);
        invalidProps.setJwtPrivateKeyLocation("file:" + invalidPrivate);

        JWKSourceAutoConfiguration invalidConfig =
                new JWKSourceAutoConfiguration(invalidProps, new DefaultResourceLoader());
        invalidConfig.afterPropertiesSet();
        assertNotNull(invalidConfig.jwkSource());
    }

    @Test
    void afterPropertiesSetHandlesUnreadableResources() {
        Oauth2Properties properties = new Oauth2Properties();
        properties.setJwtPublicKeyLocation("unreadable:public");
        properties.setJwtPrivateKeyLocation("unreadable:private");

        ResourceLoader resourceLoader =
                new ResourceLoader() {
                    @Override
                    public Resource getResource(String location) {
                        return new AbstractResource() {
                            @Override
                            public String getDescription() {
                                return "unreadable";
                            }

                            @Override
                            public boolean exists() {
                                return true;
                            }

                            @Override
                            public boolean isReadable() {
                                return true;
                            }

                            @Override
                            public InputStream getInputStream() throws IOException {
                                throw new IOException("boom");
                            }
                        };
                    }

                    @Override
                    public ClassLoader getClassLoader() {
                        return getClass().getClassLoader();
                    }
                };

        JWKSourceAutoConfiguration configuration =
                new JWKSourceAutoConfiguration(properties, resourceLoader);
        configuration.afterPropertiesSet();
    }

    @Test
    void afterPropertiesSetWarnsWhenOnlyOneKeyProvided() throws Exception {
        KeyPair keyPair = generateKeyPair();
        Path publicKeyFile = tempDir.resolve("public-only.pem");
        writePem(publicKeyFile, "PUBLIC KEY", keyPair.getPublic().getEncoded());

        Oauth2Properties properties = new Oauth2Properties();
        properties.setJwtPublicKeyLocation("file:" + publicKeyFile.toString());
        properties.setJwtPrivateKeyLocation(" ");

        JWKSourceAutoConfiguration configuration =
                new JWKSourceAutoConfiguration(properties, new DefaultResourceLoader());
        configuration.afterPropertiesSet();
        assertNotNull(configuration.jwkSource());

        Oauth2Properties privateOnly = new Oauth2Properties();
        Path privateKeyFile = tempDir.resolve("private-only.pem");
        writePem(privateKeyFile, "PRIVATE KEY", keyPair.getPrivate().getEncoded());
        privateOnly.setJwtPublicKeyLocation(" ");
        privateOnly.setJwtPrivateKeyLocation("file:" + privateKeyFile.toString());

        JWKSourceAutoConfiguration privateConfig =
                new JWKSourceAutoConfiguration(privateOnly, new DefaultResourceLoader());
        privateConfig.afterPropertiesSet();
        assertNotNull(privateConfig.jwkSource());
    }

    @Test
    void generateRsaKeyThrowsWhenAlgorithmMissing() {
        Oauth2Properties properties = new Oauth2Properties();
        JWKSourceAutoConfiguration configuration =
                new JWKSourceAutoConfiguration(properties, new DefaultResourceLoader());

        try (MockedStatic<KeyPairGenerator> mocked = Mockito.mockStatic(KeyPairGenerator.class)) {
            mocked.when(() -> KeyPairGenerator.getInstance("RSA"))
                    .thenThrow(new NoSuchAlgorithmException("RSA"));

            IllegalStateException exception =
                    assertThrows(
                            IllegalStateException.class,
                            () ->
                                    ReflectionTestUtils.invokeMethod(
                                            configuration, "generateRsaKey"));
            assertNotNull(exception.getMessage());
        }
    }

    @Test
    void usesFallbackKidWhenThumbprintComputationFails() throws Exception {
        KeyPair keyPair = generateKeyPair();
        Oauth2Properties properties = new Oauth2Properties();
        JWKSourceAutoConfiguration configuration =
                new JWKSourceAutoConfiguration(properties, new DefaultResourceLoader());
        ReflectionTestUtils.setField(
                configuration, "publicKey", (RSAPublicKey) keyPair.getPublic());
        ReflectionTestUtils.setField(
                configuration, "privateKey", (RSAPrivateKey) keyPair.getPrivate());

        try (MockedStatic<MessageDigest> mocked = Mockito.mockStatic(MessageDigest.class)) {
            mocked.when(() -> MessageDigest.getInstance("SHA-256"))
                    .thenThrow(new NoSuchAlgorithmException("SHA-256"));

            JWKSource<SecurityContext> jwkSource = configuration.jwkSource();
            assertNotNull(jwkSource);
        }
    }

    private static KeyPair generateKeyPair() throws Exception {
        KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(1024);
        return generator.generateKeyPair();
    }

    private static void writePem(Path file, String type, byte[] encoded) throws IOException {
        String content =
                "-----BEGIN "
                        + type
                        + "-----\n"
                        + Base64.getEncoder().encodeToString(encoded)
                        + "\n-----END "
                        + type
                        + "-----\n";
        Files.writeString(file, content, StandardCharsets.UTF_8);
    }
}
