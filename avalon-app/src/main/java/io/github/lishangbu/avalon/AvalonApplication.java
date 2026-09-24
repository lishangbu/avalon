package io.github.lishangbu.avalon;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** 单一启动入口；模块通过包边界组织，共用事务管理器及数据库。 */
@SpringBootApplication
public class AvalonApplication {

	public static void main(String[] args) {
		SpringApplication.run(AvalonApplication.class, args);
	}

}
