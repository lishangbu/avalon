package io.github.lishangbu.avalon.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

/// 管理应用启动类
///
/// Spring Boot 管理后台应用的入口
///
/// @author lishangbu
/// @since 2025/8/24
@EnableSpringDataWebSupport
@EnableJpaRepositories("io.github.lishangbu.avalon.**.repository")
@EntityScan("io.github.lishangbu.avalon.**.entity")
@SpringBootApplication(scanBasePackages = "io.github.lishangbu.avalon")
public class AvalonAdminApplication {

  static void main(String[] args) {
    SpringApplication.run(AvalonAdminApplication.class, args);
  }
}
