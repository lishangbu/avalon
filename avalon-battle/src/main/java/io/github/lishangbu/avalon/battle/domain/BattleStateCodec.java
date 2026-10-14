package io.github.lishangbu.avalon.battle.domain;

import tools.jackson.databind.DeserializationFeature;
import tools.jackson.core.StreamReadFeature;
import tools.jackson.databind.json.JsonMapper;

/** 正式战局存档的唯一读取边界；拒绝未知字段及已忽略的旧属性，不能静默丢弃旧状态。 */
public final class BattleStateCodec {

	/** 原始数值显式为null、拼接多份JSON或携带旧属性都属于损坏数据，必须停止读取。 */
	private static final JsonMapper JSON = JsonMapper.builder()
		.enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
		.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
		.enable(DeserializationFeature.FAIL_ON_IGNORED_PROPERTIES)
		.enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS)
		.enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES)
		.build();

	private BattleStateCodec() {
	}

	/** 单人存档只接受当前BattleState格式，不导入旧首领游标或单位状态字段。 */
	public static BattleState battle(String text) {
		return JSON.readValue(text, BattleState.class);
	}

	/** 多人存档连同嵌套战局一起严格读取，成员和提交窗口仍由宿主校验。 */
	public static DungeonRoundState dungeon(String text) {
		return JSON.readValue(text, DungeonRoundState.class);
	}

	/** 引擎预览与结算使用相同序列化规则深复制，不保留与原战局共享的可变集合。 */
	public static BattleState copy(BattleState state) {
		return battle(JSON.writeValueAsString(state));
	}

}
