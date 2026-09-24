package io.github.lishangbu.avalon.character.application.model;

/** 相邻移动中的同步事件；观察者奖励失败时，位置和奖励一起回滚。 */
public record CharacterArrived(long owner, String location) {
}
