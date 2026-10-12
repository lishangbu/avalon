package io.github.lishangbu.avalon.creature.application.model;

import io.github.lishangbu.avalon.inventory.application.model.CultivationMaterialMode;
import io.github.lishangbu.avalon.inventory.application.model.CultivationMaterialPlan;

/**
 * CreatureEvolutionPreviewService 使用的命令或结果数据，包含before、bound、customName、target等字段。
 * 模型只表达请求或执行结果，资格、并发修订和资产结算仍由正式用例检查。
 *
 * @param before 操作前预览
 * @param bound 资产是否绑定
 * @param customName 名称是否由玩家自定义
 * @param target 目标编号或目标声明
 * @param name 展示名称
 * @param materials 材料需求或材料投影
 * @param resultBound 结果资产的绑定状态
 */
public record CreatureEvolutionPreview(CreatureView before, boolean bound, boolean customName, String target,
		String name, CultivationMaterialMode mode, CultivationMaterialPlan materials, boolean resultBound) {
}
