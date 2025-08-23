package io.github.lishangbu.avalon.server;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "io.github.lishangbu.avalon")
@MapperScan(basePackages = "io.github.lishangbu.avalon.**.mapper")
public class AvalonStandaloneServerApplication {

  public static void main(String[] args) {
    SpringApplication.run(AvalonStandaloneServerApplication.class, args);
  }
}
