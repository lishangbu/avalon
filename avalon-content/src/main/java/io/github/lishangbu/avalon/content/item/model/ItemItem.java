package io.github.lishangbu.avalon.content.item.model;

/**
 * ItemCatalog 使用的类型化配置，声明id、name、description、category等数据。
 * 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param id 稳定业务编号，用于查询、关联或回执引用
 * @param name 展示名称
 * @param description 玩家可读说明
 * @param category 物品分类
 * @param type 业务分类
 * @param task 是否属于任务物品
 * @param sellPrice 出售价格
 * @param tool 工具引用
 * @param book 技能书引用
 * @param capture 捕捉声明
 * @param equipment 装备声明或装备标志
 */
public record ItemItem(String id, String name, String description, ItemCategory category, ItemConsumableType type,
		ItemScope scope, boolean task, Integer sellPrice, ItemRecovery recovery, ItemTool tool, ItemBook book,
		ItemCapture capture, ItemGear equipment) {
	public String categoryLabel() {
		return type == null ? category.title : type.title;
	}
}
