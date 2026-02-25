package io.github.lishangbu.avalon.game.calculator.growthrate;

import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/// 成长速率(GrowthRate)业务层实现类
///
/// @author lishangbu
/// @since 2026/2/11
@RequiredArgsConstructor
@Service
public class GrowthRateCalculatorFactory {
    private final List<GrowthRateCalculator> calculators;

    /// 计算达到指定等级所需的总经验值
    ///
    /// 宝可梦所拥有的经验值与其等级息息相关。同一进化家族的宝可梦都拥有相同的经验值累积速度。
    /// 但是不同进化家族的宝可梦提升到特定等级所需的经验值是不同的。
    /// 所有新获得的宝可梦都拥有达到当前所需等级的最低经验值。
    ///
    ///
    /// 根据经验值累积速度，被划分为6种，每种速度宝可梦升级所需经验值有不同的计算方式，故它们达到每一级所需的经验值是不同的。
    /// 六种经验值累计速度的“快”或者“慢”是指它们达到100级时所需的经验值总额，总额越小，累积速度越快。
    /// 其中最快和最慢两档在宝可梦初期（大约在40级以前）的成长速度完全相反：
    /// 在初期提升等级较快的宝可梦，在后期提升等级的速度就会比其他宝可梦慢得多；
    /// 而在初期提升等级较慢的宝可梦，在后期提升等级的速度就会比其他宝可梦快得多。
    ///
    /// 第一世代引进的4种速度均是通过多项式函数计算，而在第三世代引入的2种经验值速度则是一种分段函数，这六种函数都与其等级三次方相关，
    /// 第三世代引入的两种还涉及四次函数与过程中的向下取整运算。尽管有公式可以计算每一等级与其经验值的换算关系，
    /// 但是从第二世代开始，引入了计算表来计算，这是为了防止速度较慢的种类在低等级时出现的经验值计算错误。
    /// 当计算结果为小数时，无条件向下取整。⌊ ⌋表示运算中的向下取整。在等级很低（低于3级）时，此公式可能不适用。
    ///
    /// @param internalName 成长速率内部名称
    /// @param level        目标等级，必须大于0
    /// @return 达到指定等级所需的总经验值，等级无效时返回0
    public int calculateGrowthRate(String internalName, int level) {
        if (level <= 0) return 0;
        if (level == 1) {
            return 0;
        }
        return calculators.stream()
                .filter(calculator -> calculator.support(internalName))
                .findFirst()
                .map(calculator -> calculator.calculateGrowthRate(level))
                .orElse(0);
    }
}
