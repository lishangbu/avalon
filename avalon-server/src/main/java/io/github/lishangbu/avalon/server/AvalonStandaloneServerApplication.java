package io.github.lishangbu.avalon.server;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

@EnableJdbcRepositories(basePackages = "io.github.lishangbu.avalon.**.repository")
@EnableSpringDataWebSupport
@EntityScan(basePackages = "io.github.lishangbu.avalon.**.entity")
@SpringBootApplication(scanBasePackages = "io.github.lishangbu.avalon")
public class AvalonStandaloneServerApplication {

  public static void main(String[] args) {
    SpringApplication.run(AvalonStandaloneServerApplication.class, args);
  }
}
