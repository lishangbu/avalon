package io.github.lishangbu.avalon.standalone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.data.web.config.EnableSpringDataWebSupport;

@EnableSpringDataWebSupport
@EnableJpaRepositories("io.github.lishangbu.avalon.**.repository")
@EntityScan("io.github.lishangbu.avalon.**.entity")
@SpringBootApplication(scanBasePackages = "io.github.lishangbu.avalon")
public class AvalonStandaloneApplication {

  static void main(String[] args) {
    SpringApplication.run(AvalonStandaloneApplication.class, args);
  }
}
