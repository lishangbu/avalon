package io.github.lishangbu.avalon.world.api.model;

/**
 * 创角请求仅包含名称与职业意图；初始经验、位置、编号均由后端赋值。
 */
public record WorldCreate(String name, String profession) {
}
