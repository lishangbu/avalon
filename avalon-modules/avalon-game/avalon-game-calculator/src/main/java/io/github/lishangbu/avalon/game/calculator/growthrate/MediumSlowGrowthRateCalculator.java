package io.github.lishangbu.avalon.game.calculator.growthrate;

import org.springframework.stereotype.Service;

/// 较慢组成长速率计算器
///
/// @author lishangbu
/// @since 2026/2/25
@Service
public class MediumSlowGrowthRateCalculator extends AbstractGrowthRateCalculator {
  /// 获取较慢组成长速率的内部名称
  @Override
  protected String getGrowthRateInternalName() {
    return "medium-slow";
  }

  /// 较慢组：
  /// EXP = 1.2 * Lv^3 - 15 * Lv^2 + 100 * Lv - 140
  @Override
  protected int innerCalculateGrowthRate(int level) {
    return (6 * level * level * level) / 5 - 15 * level * level + 100 * level - 140;
  }
}
