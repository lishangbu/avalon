package io.github.lishangbu.avalon.world.application.model;

/**
 * LifeSkillQueryService 使用的独立数据投影，包含code、name、profession、learned等数据。
 * 服务端计算对应状态；模型的展示字段本身不授予资产领取或修改权限。
 *
 * @param code 内容目录中的稳定代码
 * @param name 展示名称
 * @param profession 职业代码
 * @param learned 本次学会的技能
 * @param level 等级
 * @param proficiency 熟练度
 */
public record LifeSkillEntry(String code, String name, boolean profession, boolean learned, int level,
		double proficiency, Long nextThreshold) {
}
