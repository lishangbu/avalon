package io.github.lishangbu.avalon.app;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 检查仓库中实际源码的分层依赖，阻止后续新增类绕过职责边界。 不检查目录数量或文件行数；只检查会影响架构的依赖方向。
 * 并发与事务语义由集成测试验证，此测试不能证明代码没有运行时写入行为。
 */
class LayerBoundaryTest {

	/**
	 * 遍历所有实际业务模块源码的import关系，禁止HTTP直接访问持久化、查询调用命令及跨模块导入实体；应用层允许直接调用统一的
	 * ObjectMapper。缺少源码不能静默跳过。
	 */
	@Test
	void applicationLayersKeepTheirDependencyDirection() throws Exception {
		Path root = Path.of("").toAbsolutePath();
		while (root != null && !Files.exists(root.resolve("avalon-ui/package.json")))
			root = root.getParent();
		assertNotNull(root, "必须在工程目录下运行，不能无源码时跳过架构校验");
		List<String> violations = new ArrayList<>();
		int checked = 0;
		for (String module : List.of("identity", "character", "world", "inventory", "creature", "battle", "market",
				"chat")) {
			Path source = root.resolve("avalon-" + module + "/src/main/java");
			try (var files = Files.walk(source)) {
				for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
					String text = Files.readString(file);
					var declaration = Pattern.compile("package ([^;]+);").matcher(text);
					assertTrue(declaration.find(), file.toString());
					String pkg = declaration.group(1);
					// 只看真正的 import，注释中讨论架构不应导致误报。
					var imports = Pattern.compile("(?m)^import (?:static )?([^;]+);").matcher(text);
					while (imports.find()) {
						String dependency = imports.group(1);
						boolean invalid = false;
						if (pkg.endsWith(".api"))
							invalid = dependency.contains(".persistence.")
									|| dependency.startsWith("jakarta.persistence.");
						if (pkg.endsWith(".application.query"))
							invalid |= dependency.contains(".application.command.");
						if (pkg.endsWith(".domain"))
							invalid |= dependency.contains(".persistence.") || dependency.contains(".application.")
									|| dependency.startsWith("jakarta.persistence.")
									|| dependency.startsWith("jakarta.ws.rs.");
						if (pkg.contains(".application."))
							invalid |= dependency.contains(".api.") || dependency.startsWith("jakarta.ws.rs.");
						if (dependency.contains(".persistence.entity.")
								&& dependency.startsWith("io.github.lishangbu.avalon.")
								&& !dependency.startsWith("io.github.lishangbu.avalon." + module + "."))
							invalid = true;
						if (invalid)
							violations.add(file.getFileName() + " -> " + dependency);
					}
					checked++;
				}
			}
		}
		assertTrue(checked > 40, "架构检查必须覆盖全部已实现业务模块");
		assertEquals(List.of(), violations, "禁止通过 Resource 访问实体、查询调用命令或跨模块暴露实体");
	}

}
