package io.github.lishangbu.avalon.dataset.service;

/// 成长速率(GrowthRate)业务层
///
/// @author lishangbu
/// @since 2026/2/11
public interface GrowthRateService {

  Integer calculateGrowthRate(Long id, int level);
}
