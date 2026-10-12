package io.github.lishangbu.avalon.battle.application.model;

import io.github.lishangbu.avalon.battle.domain.BattleStateStats;
import java.util.List;

/**
 * BattleView 使用的独立数据投影，包含id、name、element、level等数据。 服务端计算对应状态；模型的展示字段本身不授予资产领取或修改权限。
 *
 * @param id 稳定业务编号，用于查询、关联或回执引用
 * @param name 展示名称
 * @param element 元素属性
 * @param level 等级
 * @param enemy 是否属于敌方
 * @param hero 是否为角色单位
 * @param position 编队位置
 * @param hp 当前生命值
 * @param mp 当前魔力值
 * @param stats 属性数据
 * @param captured 是否已捕获
 * @param ability 特性代码
 * @param effects 效果声明列表
 * @param actions 允许的行动声明
 */
public record BattleViewUnitView(String id, String name, String element, int level, boolean enemy, boolean hero,
		int position, int hp, int mp, BattleStateStats stats, boolean captured, String ability, int shield,
		int slowRounds, List<String> effects, List<BattleViewAction> actions, List<BattleViewCaptureOption> captures,
		BattleStateStats captureStats) {
}
