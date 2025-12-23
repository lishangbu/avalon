---
applyTo: "*MapperTest.java"
---

# Mapper 层测试编写规范

## 基本要求

- 所有 Mapper 层测试类必须继承 `AbstractMapperTest` 基类
- 使用 `@MybatisPlusTest` 注解启用 MyBatis-Plus 测试支持
- 测试方法命名遵循 `should + 预期结果 + When + 测试条件` 格式
- 使用 AAA（Arrange-Act-Assert）模式组织测试代码
- 测试类和方法必须添加详细的 JavaDoc 注释

## 容器复用

- **禁止**在测试类中直接声明 `@Container` PostgreSQL 容器
- 通过继承 `AbstractMapperTest` 自动获取共享的容器实例
- 容器在所有测试开始前启动，测试结束后自动停止

## 测试类模板

```java
package io.github.lishangbu.avalon.xxx.mapper;

import com.baomidou.mybatisplus.test.autoconfigure.MybatisPlusTest;
import io.github.lishangbu.avalon.xxx.AbstractMapperTest;
import io.github.lishangbu.avalon.xxx.entity.XxxEntity;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * XXX数据访问层测试类
 * <p>
 * 测试 XxxMapper 的基本 CRUD 操作和自定义查询方法
 * 继承 AbstractMapperTest 复用 PostgreSQL 容器实例
 *
 * @author xxx
 * @since yyyy/MM/dd
 */
@MybatisPlusTest
class XxxMapperTest extends AbstractMapperTest {

  @Resource private XxxMapper xxxMapper;

  /**
   * 测试插入记录
   * <p>
   * 验证插入操作成功并返回生成的主键
   */
  @Test
  void shouldInsertEntitySuccessfully() {
    // Arrange - 准备测试数据
    XxxEntity entity = new XxxEntity();
    entity.setName("test");

    // Act - 执行测试方法
    xxxMapper.insert(entity);

    // Assert - 验证结果
    Assertions.assertNotNull(entity.getId());
    Assertions.assertTrue(entity.getId() > 0);
  }

  /**
   * 测试根据ID查询记录
   * <p>
   * 验证查询结果包含正确的数据
   */
  @Test
  void shouldFindEntityByIdWhenExists() {
    // Act
    XxxEntity entity = xxxMapper.selectById(1L);

    // Assert
    Assertions.assertNotNull(entity);
    Assertions.assertEquals(1L, entity.getId());
  }
}
```

## 抽象基类说明

每个模块应在测试根包下创建 `AbstractMapperTest` 类：

```java
package io.github.lishangbu.avalon.xxx;

import org.springframework.test.context.ContextConfiguration;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Mapper 层测试抽象基类
 * <p>
 * 提供统一的 PostgreSQL 容器实例，供所有需要数据库的测试类使用
 * 容器在所有测试开始前启动，测试结束后自动停止，提升测试执行效率
 * 所有 Mapper 层测试类应继承此类，实现容器复用
 * <p>
 * 容器启动顺序：
 * <ol>
 *   <li>Testcontainers 启动 PostgreSQL 容器</li>
 *   <li>Spring 通过 @ServiceConnection 自动配置数据源</li>
 *   <li>显式配置的 SpringLiquibase Bean 同步执行数据库迁移</li>
 *   <li>MyBatis-Plus 初始化 Mapper 接口</li>
 *   <li>测试方法开始执行</li>
 * </ol>
 *
 * @author xxx
 * @since yyyy/MM/dd
 */
@Testcontainers
@ContextConfiguration(classes = TestEnvironmentApplication.class)
public abstract class AbstractMapperTest {

}
```

## 测试环境配置类

每个模块应创建 `TestEnvironmentApplication` 配置类：

```java
package io.github.lishangbu.avalon.xxx;

import io.github.lishangbu.avalon.mybatisplus.autoconfiguration.MybatisPlusAutoConfiguration;
import liquibase.integration.spring.SpringLiquibase;
import org.springframework.boot.autoconfigure.liquibase.LiquibaseAutoConfiguration;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.testcontainers.postgresql.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;

/**
 * 测试环境自动配置类
 * <p>
 * 通过 Java 代码配置测试环境，包含 Liquibase 数据库迁移和 MyBatis-Plus 配置
 * 避免使用外部 application.yml 配置文件，提高测试的可移植性
 * <p>
 * 配置顺序：
 * <ol>
 *   <li>PostgreSQL 容器启动并暴露连接信息</li>
 *   <li>@ServiceConnection 自动配置 DataSource</li>
 *   <li>显式配置的 SpringLiquibase Bean 同步执行数据库迁移脚本</li>
 *   <li>MybatisPlusAutoConfiguration 初始化 MyBatis-Plus</li>
 * </ol>
 * <p>
 * 所有 Mapper 测试通过继承 AbstractMapperTest 自动获取此配置
 *
 * @author xxx
 * @since yyyy/MM/dd
 */
@Import(value = {LiquibaseAutoConfiguration.class, MybatisPlusAutoConfiguration.class})
@Configuration(proxyBeanMethods = false)
public class TestEnvironmentApplication {

  /**
   * PostgreSQL 测试容器 Bean
   * <p>
   * 使用 @ServiceConnection 注解，Spring Boot 会自动配置数据源连接
   * 容器在返回前已启动，确保 Liquibase 可以立即连接数据库
   *
   * @return 已启动的 PostgreSQL 容器实例
   */
  @Bean
  @ServiceConnection
  PostgreSQLContainer postgresContainer() {
    PostgreSQLContainer container =
        new PostgreSQLContainer(DockerImageName.parse("postgres:latest"));
    container.start();
    return container;
  }

  /**
   * 配置 Liquibase Bean，确保数据库迁移在测试前完成
   * <p>
   * 通过显式配置 Liquibase Bean，可以确保数据库表结构在测试执行前已经创建完成
   * Liquibase 在 Bean 初始化时同步执行迁移，避免异步初始化导致的时序问题
   *
   * @param dataSource 数据源，由 @ServiceConnection 自动配置
   * @return Liquibase 实例
   */
  @Bean
  public SpringLiquibase liquibase(DataSource dataSource) {
    SpringLiquibase liquibase = new SpringLiquibase();
    liquibase.setDataSource(dataSource);
    liquibase.setChangeLog("classpath:/db/changelog/db.changelog-master.yaml");
    liquibase.setShouldRun(true);
    return liquibase;
  }
}
```

## 注意事项

- 继承 `AbstractMapperTest` 后无需添加 `@Testcontainers` 和 `@ContextConfiguration` 注解
- 测试数据应通过 Liquibase 迁移脚本初始化
- 显式配置 Liquibase Bean 确保数据库迁移同步完成
- 测试方法应独立，不依赖执行顺序
- 每个测试方法只测试一个功能点
- 使用静态导入简化断言代码：`import static org.junit.jupiter.api.Assertions.*;`

