package io.github.lishangbu.avalon.game.calculator.growthrate;

/// 抽象的成长速率计算器
///
/// 提供 {@link GrowthRateCalculator} 的通用边界处理与名称匹配逻辑，子类需实现具体的成长曲线计算与内部名称
///
/// - 本类负责处理常见的边界情况（{@code level <= 0} 或 {@code level == 1}）并返回默认值
/// - 实际的成长值计算由子类在 {@link #tryCalculateGrowthRate(int)} 中实现
///
/// 设计要点：
/// - 名称匹配使用不区分大小写的比较，调用方可通过 {@link #support(String)} 快速判断是否支持某种成长速率
/// - 子类必须提供唯一的内部标识名（通过 {@link #getGrowthRateInternalName()}）用于匹配
///
/// @author lishangbu
/// @since 2026-02-25
public abstract class AbstractGrowthRateCalculator implements GrowthRateCalculator {

  /// 判断是否支持指定的成长速率内部名称
  ///
  /// @param growthRateInternalName 成长速率的内部名称
  /// @return 如果 {@code growthRateInternalName} 与当前实现的内部名称不区分大小写相等则返回 {@code true}，否则返回 {@code
  // false}
  /// @throws NullPointerException 当 {@code growthRateInternalName} 为 {@code null} 时会抛出该异常
  @Override
  public boolean support(String growthRateInternalName) {
    return getGrowthRateInternalName().equalsIgnoreCase(growthRateInternalName);
  }

  /// 计算指定等级的成长值
  ///
  /// 本实现负责边界处理：当 {@code level <= 0} 返回 {@code 0}；当 {@code level == 1} 返回 {@code 0}；
  /// 其余情况委托给 {@link #tryCalculateGrowthRate(int)} 由子类完成具体计算
  ///
  /// @param level 等级，预期为正整数
  /// @return 给定等级对应的成长值，边界情况（非正等级或等级为 1）返回 {@code 0}
  @Override
  public int calculateGrowthRate(int level) {
    if (level <= 0) return 0;
    if (level == 1) {
      return 0;
    } else {
      return tryCalculateGrowthRate(level);
    }
  }

  /// 子类实现：尝试计算指定等级的成长值
  ///
  /// 子类实现应假定传入的 {@code level} 已通过 {@link #calculateGrowthRate(int)} 做过基本边界处理，
  /// 因此通常从 {@code level >= 2} 开始计算具体的成长曲线
  ///
  /// @param level 等级，子类可假定为大于或等于 2 的正整数
  /// @return 给定等级对应的成长值，由子类根据具体成长曲线返回
  protected abstract int tryCalculateGrowthRate(int level);

  /// 子类必须提供的内部名称，用于 {@link #support(String)} 的匹配
  ///
  /// @return 成长速率的内部标识名，通常为小写单词或短语，用于唯一标识一种成长曲线
  protected abstract String getGrowthRateInternalName();
}
