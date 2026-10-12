package io.github.lishangbu.avalon.world;

import io.github.lishangbu.avalon.quest.QuestDefinition;
import io.github.lishangbu.avalon.quest.model.QuestDelivery;
import io.github.lishangbu.avalon.quest.model.QuestEffect;
import io.github.lishangbu.avalon.quest.model.QuestGrant;
import io.github.lishangbu.avalon.quest.model.QuestRepeat;
import io.github.lishangbu.avalon.quest.model.QuestStep;
import io.github.lishangbu.avalon.world.quest.QuestEffectHandler;
import io.github.lishangbu.avalon.world.quest.QuestExtensions;
import java.util.List;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

/** 通过注册器公开接口验证新效果无需修改引擎，未知或重复注册在启动阶段明确失败。 */
class QuestExtensionsTest {

	/** 宿主可注册新的声望奖励，校验和执行仍在同一个处理器中。 */
	@Test
	void acceptsCustomHandlerAndRejectsDuplicateOrUnknownKinds() {
		var reputation = new QuestEffectHandler() {
			public String kind() {
				return "REPUTATION";
			}

			public void validate(QuestEffect effect) {
				if (!"GUILD".equals(effect.reference()))
					throw new IllegalArgumentException("未知声望阵营");
			}

			public List<String> apply(long owner, QuestGrant grant) {
				return List.of("公会声望 +" + grant.effect().amount());
			}
		};
		var registry = new QuestExtensions(List.of(reputation), List.of());
		var definition = definition(new QuestEffect("REPUTATION", "GUILD", 10));
		assertDoesNotThrow(() -> registry.validate(definition));
		assertEquals(List.of("公会声望 +10"), registry.apply(1,
				new QuestGrant("instance:1:step:0", definition.steps().getFirst().effects().getFirst())));
		assertThrows(IllegalStateException.class,
				() -> new QuestExtensions(List.of(reputation, reputation), List.of()));
		assertThrows(IllegalArgumentException.class,
				() -> registry.validate(definition(new QuestEffect("UNKNOWN", null, 1))));
		assertThrows(IllegalArgumentException.class,
				() -> registry.validate(definition(new QuestEffect("REPUTATION", "TYPO", 1))));
	}

	/** 使用同一生产定义构造路径，测试验证完整配置，而非直接调用处理器的内部函数。 */
	private QuestDefinition definition(QuestEffect effect) {
		var step = new QuestStep("step", List.of(), List.of(), List.of(), List.of(effect), QuestDelivery.MANUAL, null,
				null);
		return new QuestDefinition("quest", false, true, QuestRepeat.ONCE, "Asia/Hong_Kong", 1, 10, null, false,
				List.of(step), List.of());
	}

}
