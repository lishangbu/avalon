package io.github.lishangbu.avalon.battle.application.model;

/**
 * FoodCommandService 使用的命令或结果数据，包含health、mana、restoredHealth、restoredMana等字段。
 * 模型只表达请求或执行结果，资格、并发修订和资产结算仍由正式用例检查。
 *
 * @param health 生命属性
 * @param mana 魔力属性
 */
public record FoodResult(int health, int mana, int restoredHealth, int restoredMana) {
}
