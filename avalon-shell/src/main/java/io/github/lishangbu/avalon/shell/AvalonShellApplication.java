package io.github.lishangbu.avalon.shell;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jdbc.repository.config.EnableJdbcRepositories;
import org.springframework.shell.command.annotation.CommandScan;

@EnableJdbcRepositories(basePackages = "io.github.lishangbu.avalon.**.repository")
@EntityScan(basePackages = "io.github.lishangbu.avalon.**.entity")
@CommandScan(basePackages = "io.github.lishangbu.avalon.shell")
@SpringBootApplication
public class AvalonShellApplication {

  public static void main(String[] args) {
    SpringApplication.run(AvalonShellApplication.class, args);
  }
}
