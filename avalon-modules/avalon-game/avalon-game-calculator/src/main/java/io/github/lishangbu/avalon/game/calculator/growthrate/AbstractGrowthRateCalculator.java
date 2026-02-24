package io.github.lishangbu.avalon.game.calculator.growthrate;

/// 抽象的成长速率计算器
///
/// @author lishangbu
/// @since 2026/2/25
public abstract class AbstractGrowthRateCalculator implements GrowthRateCalculator {
  @Override
  public boolean support(String growthRateInternalName) {
    return getGrowthRateInternalName().equalsIgnoreCase(growthRateInternalName);
  }

  /// 慢组：
  /// EXP = 1.25 * Lv^3
  @Override
  public int calculateGrowthRate(int level) {
    if (level <= 0) return 0;
    if (level == 1) {
      return 0;
    } else {
      return innerCalculateGrowthRate(level);
    }
  }

  protected abstract int innerCalculateGrowthRate(int level);

  protected abstract String getGrowthRateInternalName();
}
