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

  /// 根据成长速率ID和等级计算所需经验值
  ///
  /// 根据宝可梦的成长速率类型，使用不同的数学公式计算从等级1升至指定等级所需的总经验值
  /// 支持的成长速率类型包括：慢、较快、快、较慢、最快、最慢
  ///
  /// 实现细节：
  /// - slow（慢）：公式 (5 * level^3) / 4
  /// - medium（较快）：公式 level^3
  /// - fast（快）：公式 (4 * level^3) / 5
  /// - medium-slow（较慢）：公式 (6/5) * level^3 - 15 * level^2 + 100 * level - 140
  /// - slow-then-very-fast（最快）：分段函数，50级前慢速，68级前加速，98级前极快，之后稳定
  /// - fast-then-very-slow（最慢）：分段函数，15级前快速，35级前减速，之后缓慢
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
