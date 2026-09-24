package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.AvalonApplication;
import org.springframework.test.web.servlet.client.RestTestClient;
import org.springframework.boot.resttestclient.autoconfigure.AutoConfigureRestTestClient;
import tools.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.AfterEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.transaction.PlatformTransactionManager;
import org.testcontainers.postgresql.PostgreSQLContainer;

/**
 * 全部真实集成测试共用隔离容器和随机 HTTP 端口，不读取 local 数据库。 不为测试类包裹自动回滚事务：并发请求及跨服务提交必须真实发生。
 */
@SpringBootTest(classes = AvalonApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@AutoConfigureRestTestClient
@Import(TestDoubles.class)
abstract class SpringIntegrationTest {

	@ServiceConnection
	static final PostgreSQLContainer DATABASE = new PostgreSQLContainer("postgres:17-alpine");
	static {
		DATABASE.start();
	}

	/** 只在测试上下文替换启动入口，业务事务测试不受后台扫描干扰；打包进程仍运行真实调度。 */
	@MockitoBean(name = "maintenanceStartup")
	ApplicationRunner maintenanceStartup;

	@LocalServerPort
	int port;

	@Autowired
	PlatformTransactionManager transactionManager;

	TestTransactions transactions;

	@Autowired
	RestTestClient client;

	@Autowired
	JsonMapper jsonMapper;

	@BeforeEach
	void prepareInfrastructure() {
		transactions = new TestTransactions(transactionManager);
		TestDoubles.reset();
	}

	@AfterEach
	void restoreInfrastructure() {
		TestDoubles.reset();
	}

}
