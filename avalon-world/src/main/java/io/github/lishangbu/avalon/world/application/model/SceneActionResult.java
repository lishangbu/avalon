package io.github.lishangbu.avalon.world.application.model;

/** quantity只表示本请求首次新增的任务物品数量；已完成后的新回看请求为0。 */
public record SceneActionResult(String actionId, boolean completed, String dialogue, String itemCode, int quantity) {
}
