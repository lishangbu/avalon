package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.battle.application.command.DungeonBattleCommandService;
import io.github.lishangbu.avalon.battle.application.command.DungeonRunCommandService;
import io.github.lishangbu.avalon.battle.domain.BattleEngineResources;
import io.github.lishangbu.avalon.battle.domain.DungeonRoundStateSubmission;
import java.time.Instant;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** 集成测试显式生成新协议输入，相同成员同轮复用UUID，不在生产服务保留旧提交入口。 */
final class DungeonTestCommands {

	private DungeonTestCommands() {
	}

	/** 持久化用例发送完整请求与修订，响应丢失时重试沿用相同编号。 */
	static void submit(DungeonBattleCommandService commands, long battle, long character, int round,
			DungeonRoundStateSubmission input, Instant now, BattleEngineResources resources) {
		commands.submit(battle, character, request(battle, character, round), round - 1, round, input, now, resources);
	}

	/** 副本进度用例同样通过唯一新协议进入成员提交事务。 */
	static void submit(DungeonRunCommandService commands, long run, long owner, long battle, int round,
			DungeonRoundStateSubmission input) {
		commands.submit(run, owner, battle, request(battle, owner, round), round - 1, round, input);
	}

	/** 稳定测试UUID由战局、成员和轮次确定，异参重试必须交由服务端拒绝。 */
	private static UUID request(long battle, long member, int round) {
		return UUID.nameUUIDFromBytes(
				("dungeon-test:" + battle + ":" + member + ":" + round).getBytes(StandardCharsets.UTF_8));
	}

}
