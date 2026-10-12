package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.content.world.WorldCatalog;
import io.github.lishangbu.avalon.content.world.model.WorldDefinition;
import io.github.lishangbu.avalon.content.world.model.WorldPlace;
import io.github.lishangbu.avalon.content.world.model.WorldProfession;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 验证配置入口拒绝无法运行的地图，避免内容错误在玩家请求时才暴露。
 */
class ContentValidationTest {

	/**
	 * 从合法地图派生非法引用或不可达布局，验证道路连接检查。测试只涉及启动内容，不通过运行时传送修正坏配置。
	 */
	@Test
	void rejectsBrokenReferencesAndDisconnectedMap() {
		var jobs = List.of(new WorldProfession("WARRIOR", "战士", "近战"));
		var broken = new WorldPlace("a", "起点", "描述", 0, 0, List.of("missing"));
		assertThrows(IllegalArgumentException.class,
				() -> WorldCatalog.validate(new WorldDefinition("a", jobs, List.of(broken))));
		var a = new WorldPlace("a", "起点", "描述", 0, 0, List.of());
		var b = new WorldPlace("b", "孤岛", "描述", 1, 0, List.of());
		assertThrows(IllegalArgumentException.class,
				() -> WorldCatalog.validate(new WorldDefinition("a", jobs, List.of(a, b))));
		assertThrows(IllegalArgumentException.class,
				() -> WorldCatalog.validate(new WorldDefinition("a", jobs, List.of(a, a))));
	}

}
