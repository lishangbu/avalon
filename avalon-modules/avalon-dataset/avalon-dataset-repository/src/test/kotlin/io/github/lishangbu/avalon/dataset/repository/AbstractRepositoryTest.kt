package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.TestEnvironmentApplication
import org.springframework.boot.test.context.SpringBootTest
import org.testcontainers.junit.jupiter.Testcontainers

/**
 * 仓储测试抽象基类
 *
 * 为数据集仓储测试提供统一的 Spring Boot 与 Testcontainers 环境
 */
@Testcontainers
@SpringBootTest(classes = [TestEnvironmentApplication::class], webEnvironment = SpringBootTest.WebEnvironment.NONE)
abstract class AbstractRepositoryTest
