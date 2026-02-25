package io.github.lishangbu.avalon.game.calculator.growthrate;

import org.springframework.stereotype.Service;

/// 慢组成长速率计算器
///
/// @author lishangbu
/// @since 2026/2/25
@Service
public class SlowGrowthRateCalculator extends AbstractGrowthRateCalculator {
  /// 获取慢组成长速率的内部名称
  @Override
  protected String getGrowthRateInternalName() {
    return "slow";
  }

  /// 慢组：
  /// EXP = 1.25 * Lv^3
  @Override
  protected int tryCalculateGrowthRate(int level) {
    return (5 * level * level * level) / 4;
  }
}
