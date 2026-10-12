package io.github.lishangbu.avalon.character.application.model;

/**
 * CharacterQueryService 使用的独立数据投影，包含dungeon、battle、preparationRevision、course等数据。
 * 服务端计算对应状态；模型的展示字段本身不授予资产领取或修改权限。
 *
 * @param dungeon 副本声明
 * @param battle 战局引用或战局数据
 * @param course 课程引用
 */
public record CharacterAdventureState(Long dungeon, Long battle, long preparationRevision, Long course) {
	public CharacterAdventureState(Long dungeon, Long battle, long preparationRevision) {
		this(dungeon, battle, preparationRevision, null);
	}
}
