package io.github.lishangbu.avalon.dataset.service.impl;

import io.github.lishangbu.avalon.dataset.entity.GrowthRate;
import io.github.lishangbu.avalon.dataset.repository.GrowthRateRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/// 成长速率(GrowthRate)业务层实现类
///
/// @author lishangbu
/// @since 2026/2/11
@RequiredArgsConstructor
@Service
public class GrowthRateService
    implements io.github.lishangbu.avalon.dataset.service.GrowthRateService {
  private final GrowthRateRepository growthRateRepository;

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
