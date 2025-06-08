package io.github.lishangbu.avalon.pokeapi.model.pokemon;

/**
 * 成长速率经验等级，记录达到特定等级所需的经验值
 *
 * @param level 获得的等级
 * @param experience 达到所引用等级所需的经验值
 * @author lishangbu
 * @since 2025/6/8
 */
public record GrowthRateExperienceLevel(Integer level, Integer experience) {}
