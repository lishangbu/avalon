package io.github.lishangbu.avalon.game.calculator.growthrate;

import org.springframework.stereotype.Service;

/// 快组成长速率计算器
///
/// @author lishangbu
/// @since 2026/2/25
@Service
public class FastGrowthRateCalculator extends AbstractGrowthRateCalculator {
  /// 获取快组成长速率的内部名称
  @Override
  protected String getGrowthRateInternalName() {
    return "fast";
  }

  /// 快组：
  /// EXP = 0.8 * Lv^3
  @Override
  protected int innerCalculateGrowthRate(int level) {
    return (4 * level * level * level) / 5;
  }
}
