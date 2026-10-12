package io.github.lishangbu.avalon.creature.application.model;

/** 雪花编号使用字符串；未知旧档保持空值，不把当前持有者猜成原捕获者。 */
public record CreatureProvenance(boolean bound, String originKind, String originId, String originName, String captorId,
		String captorName) {
}
