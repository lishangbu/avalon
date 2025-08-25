package io.github.lishangbu.avalon.admin;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 管理应用
 *
 * @author lishangbu
 * @since 2025/8/24
 */
@SpringBootApplication(scanBasePackages = "io.github.lishangbu.avalon")
@MapperScan(basePackages = "io.github.lishangbu.avalon.**.mapper")
public class AvalonAdminApplication {

  public static void main(String[] args) {
    SpringApplication.run(AvalonAdminApplication.class, args);
  }
}
