package io.github.lishangbu.avalon.battle.application.model;

/**
 * DungeonRunView 使用的独立数据投影，包含position、name、cleared、rest等数据。
 * 服务端计算对应状态；模型的展示字段本身不授予资产领取或修改权限。
 *
 * @param position 编队位置
 * @param name 展示名称
 * @param rest 恢复声明
 */
public record DungeonRunViewNode(int position, String name, boolean cleared, boolean rest) {
}
