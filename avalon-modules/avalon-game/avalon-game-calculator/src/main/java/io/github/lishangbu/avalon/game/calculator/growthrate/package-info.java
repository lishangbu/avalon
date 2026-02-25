/// 成长速率计算器相关类的包
///
/// 本包提供与成长速率（growth rate）计算相关的接口与实现，用于计算给定等级对应的成长值
///
/// 主要职责：
/// - 提供 {@link io.github.lishangbu.avalon.game.calculator.growthrate.GrowthRateCalculator}
// 接口，定义计算契约
/// - 提供抽象基类 {@link
// io.github.lishangbu.avalon.game.calculator.growthrate.AbstractGrowthRateCalculator}，包含通用边界处理与名称匹配逻辑
/// - 包含若干具体实现以支持不同的成长曲线，使用类名或内部标识进行选择与匹配
///
/// 设计与使用约定：
/// - 包内对空性的默认约定由 {@link org.jspecify.annotations.NullMarked} 注解控制，方法参数与返回值默认不可为 {@code null}
/// - 公共 API 应提供清晰的输入输出与异常说明，抛出常见运行时异常时需在实现处记录 {@code @throws}
/// - 扩展时建议通过继承 {@link
// io.github.lishangbu.avalon.game.calculator.growthrate.AbstractGrowthRateCalculator}
// 实现具体的成长曲线计算逻辑，并重写 {@code getGrowthRateInternalName} 提供唯一标识
///
/// 并发与线程安全：
/// - 本包的实现通常为无状态或仅包含只读数据，因此在并发场景下可安全复用实例；若实现包含可变状态需在类级注释中说明线程模型
///
/// @author lishangbu
/// @since 2026/02/25
@NullMarked
package io.github.lishangbu.avalon.game.calculator.growthrate;

import org.jspecify.annotations.NullMarked;
