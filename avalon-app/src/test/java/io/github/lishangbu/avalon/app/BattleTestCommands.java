package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.battle.application.command.BattleCommandService;
import io.github.lishangbu.avalon.battle.application.model.BattleView;
import io.github.lishangbu.avalon.battle.domain.BattleStateOrder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** 集成测试命令工厂：明确生成新协议请求号和修订，不在生产服务保留无UUID入口。 */
final class BattleTestCommands {

	private BattleTestCommands() {
	}

	/** 无补位场景仍发送完整载荷；同一测试战局轮次使用稳定UUID来验证真实重试。 */
	static BattleView turn(BattleCommandService commands, long owner, long battle, int round,
			List<BattleStateOrder> orders, boolean surrender) {
		return turn(commands, owner, battle, round, orders, surrender, Map.of());
	}

	/** 测试场景每完成一轮增加一次修订，载荷改变时服务器必须拒绝同一UUID。 */
	static BattleView turn(BattleCommandService commands, long owner, long battle, int round,
			List<BattleStateOrder> orders, boolean surrender, Map<String, Integer> replacements) {
		// 载荷相同才复用UUID；测试改变行动时应模拟新客户端请求，而非伪造错误重试。
		UUID request = UUID
			.nameUUIDFromBytes(("test:" + battle + ":" + round + ":" + orders + ":" + surrender + ":" + replacements)
				.getBytes(StandardCharsets.UTF_8));
		return commands.turn(owner, battle, request, round - 1, round, orders, surrender, replacements);
	}

}
