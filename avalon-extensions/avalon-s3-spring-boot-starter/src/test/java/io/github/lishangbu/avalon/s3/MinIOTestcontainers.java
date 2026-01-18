package io.github.lishangbu.avalon.s3;

import org.junit.jupiter.api.extension.BeforeAllCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.testcontainers.containers.MinIOContainer;
import org.testcontainers.utility.DockerImageName;

/// 测试环境自动配置
///
/// 本配置类为单元/集成测试提供一组轻量的运行时支持组件，目的是让测试能够在本地或 CI 中
/// 可复现地运行，而无需依赖外部环境配置
///
/// 提供的能力：
/// - 启动并暴露一个 MinIO 测试容器（用于 S3 相关集成测试）
/// - 通过 `@ServiceConnection` 将容器自动绑定到 Spring Boot 的服务连接点
///
/// 使用说明：
/// 1. 将测试类标注为使用该自动配置或继承提供该配置的测试基类（例如 `AbstractMapperTest`）
/// 2. Spring Boot 会识别 `@ServiceConnection` 并自动注入容器连接信息到相应的 bean（例如 S3 客户端）
///
/// 设计要点：
/// - 仅在测试范围内加载该配置（位于 `src/test/java`），避免影响生产代码
/// - 使用最新的官方镜像（`minio/minio:latest`），容器立即启动并由 Testcontainers 管理其生命周期
///
/// @author lishangbu
/// @since 2025/8/20
public class MinIOTestcontainers implements BeforeAllCallback {
  private static final MinIOContainer CONTAINER = new MinIOContainer(DockerImageName.parse("minio/minio:latest"))
    .withUserName("testuser")
    .withPassword("testpassword")
    .withReuse(true);

  @Override
  public void beforeAll(ExtensionContext context) {
    CONTAINER.start();

    Integer mappedPort = CONTAINER.getFirstMappedPort();
    String url = String.format("http://%s:%s", CONTAINER.getHost(), mappedPort);
    // 将 Endpoint 与凭证暴露为系统属性，便于 Spring Boot 的 ConfigurationProperties 绑定
    System.setProperty("s3.endpoint", url);
    System.setProperty("s3.accessKey", "testuser");
    System.setProperty("s3.secretKey", "testpassword");
  }

  /// 将运行中的 MinIO 容器作为 Spring Bean 暴露，方便在测试上下文中注入
  @Bean
  @ServiceConnection
  public MinIOContainer minIOContainer() {
    return CONTAINER;
  }
}
