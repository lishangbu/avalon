package io.github.lishangbu.avalon.authorization.repository;

import io.github.lishangbu.avalon.authorization.TestEnvironmentApplication;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

/// Repository 层测试抽象基类
///
/// 提供统一的 PostgreSQL 容器实例，供所有需要数据库的测试类使用
/// 容器在所有测试开始前启动，测试结束后自动停止，提升测试执行效率
/// 所有 Repository 层测试类应继承此类，实现容器复用
///
/// 容器启动顺序：
///
/// 1. Testcontainers 启动 PostgreSQL 容器
/// 2. Spring 通过 @ServiceConnection 自动配置数据源
/// 3. Spring Data Jpa 初始化 Repository 接口,创建表结构,执行数据库迁移
/// 4. 测试方法开始执行
///
/// @author lishangbu
/// @since 2025/12/23
@Testcontainers
@DataJpaTest
@ContextConfiguration(classes = TestEnvironmentApplication.class)
public abstract class AbstractRepositoryTest {}
