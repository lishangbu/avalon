package io.github.lishangbu.avalon.game.calculator.growthrate;

import org.springframework.stereotype.Service;

/// 最慢组成长速率计算器
///
/// @author lishangbu
/// @since 2026/2/25
@Service
public class FastThenVerySlowGrowthRateCalculator extends AbstractGrowthRateCalculator {
  /// 获取最慢组成长速率的内部名称
  @Override
  protected String getGrowthRateInternalName() {
    return "fast-then-very-slow";
  }

  /// 最慢组：
  /// 分段函数
  /// Lv≤15：EXP = 0.02 * Lv^3 * ⌊ (Lv+73) / 3 ⌋
  /// 16≤Lv≤36：EXP = 0.02 * Lv4 +0.28* Lv^3
  /// 37≤Lv≤100：EXP = 0.02 * Lv^3 * ⌊ (Lv + 64) / 2 ⌋
  @Override
  protected int tryCalculateGrowthRate(int level) {
    int cubedLevel = level * level * level;
    if (level <= 15) {
      int term = 24 + ((level + 1) / 3);
      return cubedLevel * term / 50;
    } else if (level <= 35) {
      int term = 14 + level;
      return cubedLevel * term / 50;
    } else {
      int term = 32 + (level / 2);
      return cubedLevel * term / 50;
    }
  }
}
