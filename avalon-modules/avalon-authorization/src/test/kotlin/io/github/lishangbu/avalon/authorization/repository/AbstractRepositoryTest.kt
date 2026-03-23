package io.github.lishangbu.avalon.authorization.repository

import io.github.lishangbu.avalon.authorization.TestEnvironmentApplication
import org.springframework.boot.test.context.SpringBootTest
import org.springframework.test.context.ContextConfiguration
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * 仓储测试抽象基类
 *
 * 为授权模块仓储测试提供统一的 Spring Boot 与 Testcontainers 环境
 */
@Testcontainers
@SpringBootTest(classes = [TestEnvironmentApplication::class], webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ContextConfiguration(classes = [TestEnvironmentApplication::class])
abstract class AbstractRepositoryTest
