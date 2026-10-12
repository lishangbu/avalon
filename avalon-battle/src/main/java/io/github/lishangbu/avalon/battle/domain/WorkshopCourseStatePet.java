package io.github.lishangbu.avalon.battle.domain;

/** 个体身份、生命和魔力跨存取保持，不借仓库操作补血或创建副本。 */
public record WorkshopCourseStatePet(String id, String name, int position, boolean locked, int hp, int mp) {
	public WorkshopCourseStatePet at(int value) {
		return new WorkshopCourseStatePet(id, name, value, locked, hp, mp);
	}

	public WorkshopCourseStatePet protectedPet() {
		return new WorkshopCourseStatePet(id, name, position, true, hp, mp);
	}
}
