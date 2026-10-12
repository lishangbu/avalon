package io.github.lishangbu.avalon.world.api.model;

/**
 * 移动请求携带预期版本用于冲突检查，不包含可任意设置的角色坐标。
 */
public record WorldMove(String destination, long expectedRevision) {
}
