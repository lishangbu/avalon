package io.github.lishangbu.avalon.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 管理应用
 *
 * @author lishangbu
 * @since 2025/8/24
 */
@SpringBootApplication(scanBasePackages = "io.github.lishangbu.avalon")
public class AvalonAdminApplication {

  static void main(String[] args) {
    SpringApplication.run(AvalonAdminApplication.class, args);
  }
}
