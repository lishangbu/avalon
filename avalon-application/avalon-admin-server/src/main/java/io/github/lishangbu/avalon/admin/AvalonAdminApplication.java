package io.github.lishangbu.avalon.admin;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

/**
 * 管理应用
 *
 * @author lishangbu
 * @since 2025/8/24
 */
@EnableSpringDataWebSupport
@EnableJdbcRepositories("io.github.lishangbu.avalon.**.repository")
@EntityScan("io.github.lishangbu.avalon.**.entity")
@SpringBootApplication(scanBasePackages = "io.github.lishangbu.avalon")
public class AvalonAdminApplication {

  public static void main(String[] args) {
    SpringApplication.run(AvalonAdminApplication.class, args);
  }
}
