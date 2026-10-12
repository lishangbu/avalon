package io.github.lishangbu.avalon.battle.application.model;

import java.util.List;

/**
 * DungeonRecruitmentQueryService 使用的独立数据投影，包含id、name、cleared、stages等数据。
 * 服务端计算对应状态；模型的展示字段本身不授予资产领取或修改权限。
 *
 * @param id 稳定业务编号，用于查询、关联或回执引用
 * @param name 展示名称
 * @param stages 阶段声明列表
 * @param members 成员列表
 * @param capacity 容量
 */
public record DungeonRecruitmentReplacement(String id, String name, int cleared, int stages, int members, int capacity,
		List<String> professions) {
}
