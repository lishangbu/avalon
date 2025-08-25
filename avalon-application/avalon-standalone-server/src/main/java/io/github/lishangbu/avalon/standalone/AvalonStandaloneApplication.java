package io.github.lishangbu.avalon.standalone;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "io.github.lishangbu.avalon")
@MapperScan(basePackages = "io.github.lishangbu.avalon.**.mapper")
public class AvalonStandaloneApplication {

  public static void main(String[] args) {
    SpringApplication.run(AvalonStandaloneApplication.class, args);
  }
}
