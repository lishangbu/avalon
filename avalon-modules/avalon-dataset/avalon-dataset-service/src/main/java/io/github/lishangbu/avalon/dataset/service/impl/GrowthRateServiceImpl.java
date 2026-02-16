package io.github.lishangbu.avalon.dataset.service.impl;

import io.github.lishangbu.avalon.dataset.entity.GrowthRate;
import io.github.lishangbu.avalon.dataset.repository.GrowthRateRepository;
import io.github.lishangbu.avalon.dataset.service.GrowthRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/// 成长速率(GrowthRate)业务层实现类
///
/// @author lishangbu
/// @since 2026/2/11
@RequiredArgsConstructor
@Service
public class GrowthRateServiceImpl implements GrowthRateService {
  private final GrowthRateRepository growthRateRepository;

  /// 计算达到指定等级所需的总经验值
  ///
  ///
  // 宝可梦所拥有的经验值与其等级息息相关。同一进化家族的宝可梦都拥有相同的经验值累积速度。但是不同进化家族的宝可梦提升到特定等级所需的经验值是不同的。所有新获得的宝可梦都拥有达到当前所需等级的最低经验值。
  ///
  ///
  // 根据经验值累积速度，被划分为6种，每种速度宝可梦升级所需经验值有不同的计算方式，故它们达到每一级所需的经验值是不同的。六种经验值累计速度的“快”或者“慢”是指它们达到100级时所需的经验值总额，总额越小，累积速度越快。其中最快和最慢两档在宝可梦初期（大约在40级以前）的成长速度完全相反：在初期提升等级较快的宝可梦，在后期提升等级的速度就会比其他宝可梦慢得多；而在初期提升等级较慢的宝可梦，在后期提升等级的速度就会比其他宝可梦快得多。
  ///
  ///
  // 第一世代引进的4种速度均是通过多项式函数计算，而在第三世代引入的2种经验值速度则是一种分段函数，这六种函数都与其等级三次方相关，第三世代引入的两种还涉及四次函数与过程中的向下取整运算。尽管有公式可以计算每一等级与其经验值的换算关系，但是从第二世代开始，引入了计算表来计算，这是为了防止速度较慢的种类在低等级时出现的经验值计算错误。 升到某一级别的累积经验值计算公式如下：
  /// <pre>
  /// Lv：代表宝可梦升级后的等级
  /// EXP：代表累积经验值
  /// 当计算结果为小数时，无条件向下取整。⌊ ⌋表示运算中的向下取整。在等级很低（低于3级）时，此公式可能不适用。
  ///
  /// 最快组：
  /// 分段函数
  /// Lv≤50：EXP = -0.02 * Lv4 + 2 * Lv^3
  /// 51≤Lv≤68：EXP = -0.01 * Lv4 + 1.5 * Lv^3
  /// 69≤Lv≤98：EXP = 0.002 * Lv^3 * ⌊ (1911 - 10 * Lv) / 3 ⌋
  /// 99≤Lv≤100：EXP = -0.01 * Lv4 + 1.6 * Lv^3
  /// 快组：
  /// EXP = 0.8 * Lv^3
  /// 较快组：
  /// EXP = Lv^3
  /// 较慢组：
  /// EXP = 1.2 * Lv^3 - 15 * Lv2 + 100 * Lv - 140
  /// 慢组：
  /// EXP = 1.25 * Lv^3
  /// 最慢组：
  /// 分段函数
  /// Lv≤15：EXP = 0.02 * Lv^3 * ⌊ (Lv+73) / 3 ⌋
  /// 16≤Lv≤36：EXP = 0.02 * Lv4 +0.28* Lv^3
  /// 37≤Lv≤100：EXP = 0.02 * Lv^3 * ⌊ (Lv + 64) / 2 ⌋
  /// </pre>
  ///
  /// @param id    成长速率ID，用于查询对应的成长速率类型
  /// @param level 目标等级，必须大于0
  /// @return 达到指定等级所需的总经验值，等级无效时返回0
  @Override
  public Integer calculateGrowthRate(Long id, int level) {
    if (level <= 0) return 0;
    if (level == 1) {
      return 0;
    }
    String internalName =
        growthRateRepository.findById(id).map(GrowthRate::getInternalName).orElse("");
    switch (internalName) {
      case "slow" -> {
        return (5 * level * level * level) / 4;
      }
      case "medium" -> {
        return level * level * level;
      }
      case "fast" -> {
        return (4 * level * level * level) / 5;
      }
      case "medium-slow" -> {
        return (6 * level * level * level) / 5 - 15 * level * level + 100 * level - 140;
      }
      case "slow-then-very-fast" -> {
        int cubedLevel = level * level * level;
        if (level <= 50) {
          return cubedLevel * (100 - level) / 50;
        } else if (level <= 68) {
          return cubedLevel * (150 - level) / 100;
        } else if (level <= 98) {
          int mod = level % 3;
          int term = 1274 + mod * mod - 9 * mod - 20 * (level / 3);
          return cubedLevel * term / 1000;
        } else {
          return cubedLevel * (160 - level) / 100;
        }
      }
      case "fast-then-very-slow" -> {
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
      default -> {
        return 0;
      }
    }
  }
}
