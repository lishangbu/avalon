package io.github.lishangbu.avalon.content.battle.model;

/**
 * TrainingCatalog 使用的类型化配置，声明name、health、attack、defense等数据。
 * 目录加载器负责引用及组合规则校验；此模型保留原字段、默认值和构造约束。
 *
 * @param name 展示名称
 * @param health 生命属性
 * @param attack 攻击属性或通道
 * @param defense 防御属性或通道
 * @param speed 速度属性
 * @param power 威力
 * @param count 业务计数
 */
public record TrainingOpponent(String name, int health, int attack, int defense, int speed, int power, int count) {
}
