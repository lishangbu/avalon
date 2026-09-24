package io.github.lishangbu.avalon.character.application.model;

/**
 * 角色查询及写后返回的稳定读模型，不包含 JPA 实体或数据库会话。 id 是浏览器安全的字符串雪花编号，revision 是移动冲突检查使用的实体版本， experience
 * 保存累计值，level 由统一成长规则派生。客户端不能通过回传此对象覆盖角色。
 */
public record CharacterView(String id, String name, String profession, String location, long revision, long experience,
		int level, String activeBattleId, long gold, String activeCourseId) {
	/** 无课程上下文的调用保留原构造方式；正式投影始终读取角色当前课程。 */
	public CharacterView(String id, String name, String profession, String location, long revision, long experience,
			int level, String activeBattleId, long gold) {
		this(id, name, profession, location, revision, experience, level, activeBattleId, gold, null);
	}
}
