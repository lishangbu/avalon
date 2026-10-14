package io.github.lishangbu.avalon.app;

import io.github.lishangbu.avalon.battle.domain.CaveMechanism;
import io.github.lishangbu.avalon.battle.domain.BattleState;
import io.github.lishangbu.avalon.battle.domain.BattleStateCodec;
import io.github.lishangbu.avalon.battle.domain.BattleStateUnit;
import io.github.lishangbu.avalon.battle.domain.DungeonRoundState;
import java.util.List;
import java.time.Instant;
import org.junit.jupiter.api.Test;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
import static org.junit.jupiter.api.Assertions.*;

/** 正式存档必须拒绝旧字段，防止重连静默丢弃中毒、首领阶段等已保存数据。 */
class BattleStateCodecTest {

	/** 单人和嵌套多人存档都拒绝被JsonIgnore屏蔽的旧属性以及未知字段。 */
	@Test
	void rejectsOldAndUnknownFieldsAtEveryStateBoundary() {
		assertThrows(JacksonException.class, () -> BattleStateCodec.battle("{\"bossPhase\":2}"));
		assertThrows(JacksonException.class, () -> BattleStateCodec.battle("{\"units\":[{\"poisonStrength\":100}]}"));
		assertThrows(JacksonException.class, () -> BattleStateCodec.dungeon("{\"battle\":{\"bossCursor\":3}}"));
		assertThrows(JacksonException.class, () -> BattleStateCodec.battle("{\"unknownState\":1}"));
		assertThrows(JacksonException.class, () -> BattleStateCodec.battle("{} {}"));
		assertThrows(JacksonException.class, () -> BattleStateCodec.battle("{\"round\":1,\"round\":2}"));
		assertThrows(JacksonException.class, () -> BattleStateCodec.battle("{\"round\":null}"));
	}

	/** 新状态的机制、统一状态及多人截止时间可完整往返，隔离副本不共享原单位。 */
	@Test
	void preservesCurrentStateAndCopiesMutableUnits() {
		var state = new BattleState();
		new CaveMechanism(state).setBossPhase(2);
		var unit = new BattleStateUnit();
		unit.setId("enemy");
		unit.setPoisonStrength(100);
		unit.setPoisonUntil(3);
		state.setUnits(List.of(unit));
		var copy = BattleStateCodec.copy(state);
		assertEquals(2, new CaveMechanism(copy).getBossPhase());
		assertEquals(100, copy.getUnits().getFirst().getPoisonStrength());
		copy.getUnits().getFirst().setPoisonStrength(200);
		assertEquals(100, unit.getPoisonStrength());
		var dungeon = new DungeonRoundState();
		dungeon.setBattle(state);
		dungeon.setDeadline(Instant.parse("2026-10-14T08:00:00Z"));
		var decoded = BattleStateCodec.dungeon(JsonMapper.builder().build().writeValueAsString(dungeon));
		assertEquals(dungeon.getDeadline(), decoded.getDeadline());
		assertEquals(2, new CaveMechanism(decoded.getBattle()).getBossPhase());
	}

}
