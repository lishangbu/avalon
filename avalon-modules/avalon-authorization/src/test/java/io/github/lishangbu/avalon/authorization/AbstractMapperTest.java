package io.github.lishangbu.avalon.authorization;

import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Mapper 层测试抽象基类
 *
 * <p>提供统一的 PostgreSQL 容器实例，供所有需要数据库的测试类使用 容器在所有测试开始前启动，测试结束后自动停止，提升测试执行效率 所有 Mapper
 * 层测试类应继承此类，实现容器复用
 *
 * <p>容器启动顺序：
 *
 * <ol>
 *   <li>Testcontainers 启动 PostgreSQL 容器
 *   <li>Spring 通过 @ServiceConnection 自动配置数据源
 *   <li>LiquibaseAutoConfiguration 执行数据库迁移，创建表结构
 *   <li>MyBatis-Plus 初始化 Mapper 接口
 *   <li>测试方法开始执行
 * </ol>
 *
 * <p>使用示例：
 *
 * <pre>{@code
 * @MybatisPlusTest
 * class UserMapperTest extends AbstractMapperTest {
 *     @Resource
 *     private UserMapper userMapper;
 *
 *     @Test
 *     void shouldInsertUserSuccessfully() {
 *         // 测试代码
 *     }
 * }
 * }</pre>
 *
 * @author lishangbu
 * @since 2025/12/23
 */
@Testcontainers
@ContextConfiguration(classes = TestEnvironmentApplication.class)
public abstract class AbstractMapperTest {}
