package io.github.lishangbu.avalon.shell;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.shell.command.annotation.CommandScan;

@EnableJpaRepositories(basePackages = "io.github.lishangbu.avalon.**.repository")
@EntityScan(basePackages = "io.github.lishangbu.avalon.**.entity")
@CommandScan(basePackages = "io.github.lishangbu.avalon.shell")
@SpringBootApplication
public class AvalonShellApplication {

  public static void main(String[] args) {
    SpringApplication.run(AvalonShellApplication.class, args);
  }
}
