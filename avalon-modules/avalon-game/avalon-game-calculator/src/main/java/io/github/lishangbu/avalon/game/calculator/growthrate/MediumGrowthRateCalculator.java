package io.github.lishangbu.avalon.game.calculator.growthrate;

import org.springframework.stereotype.Service;

/// 较快组成长速率计算器
///
/// @author lishangbu
/// @since 2026/2/25
@Service
public class MediumGrowthRateCalculator extends AbstractGrowthRateCalculator {
    /// 获取较快组成长速率的内部名称
    @Override
    protected String getGrowthRateInternalName() {
        return "medium";
    }

    /// 较快组：
    /// EXP = Lv^3
    @Override
    protected int tryCalculateGrowthRate(int level) {
        return level * level * level;
    }
}
