package io.github.lishangbu.avalon.creature.application.model;

import java.util.List;

/**
 * CreatureCustodyQueryService 使用的独立数据投影，包含id、species、speciesName、name等数据。
 * 服务端计算对应状态；模型的展示字段本身不授予资产领取或修改权限。
 *
 * @param id 稳定业务编号，用于查询、关联或回执引用
 * @param species 幻兽物种代码
 * @param speciesName 物种展示名称
 * @param name 展示名称
 * @param customName 名称是否由玩家自定义
 * @param element 元素属性
 * @param level 等级
 * @param experience 经验数量
 * @param abilitySlot 特性槽位
 * @param ability 特性代码
 * @param skills 技能声明列表
 * @param bound 资产是否绑定
 * @param captorId 捕获角色编号
 * @param captorName 捕获角色名称
 */
public record CreatureCustodyDetail(String id, String species, String speciesName, String name, boolean customName,
		String element, int level, String experience, String abilitySlot, String ability, List<String> skills,
		boolean bound, String captorId, String captorName, CreatureResources.Resources resources) {
}
