package io.github.lishangbu.avalon.standalone;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "io.github.lishangbu.avalon")
public class AvalonStandaloneApplication {

  static void main(String[] args) {
    SpringApplication.run(AvalonStandaloneApplication.class, args);
  }
}
