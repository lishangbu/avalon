package io.github.lishangbu.avalon.chat.application.model;

import java.time.Instant;
import java.util.List;

/** 浏览器使用的不可变聊天投影。角色编号与消息编号均为字符串，绝不输出账号编号或持久化对象。 */
public final class ChatViews {

	private ChatViews() {
	}

	/**
	 * 一条鉴权后的消息投影。正文保持纯文本，发送者公开名为发送时快照，不查询对方账号资料。
	 *
	 * @param id 消息雪花编号；仅作稳定引用和分页，不能当作数据库提交水位。
	 * @param channel WORLD为全服玩家频道，PRIVATE为本人参与的私聊，SYSTEM为可信游戏通知。
	 * @param sender 发件角色编号，系统为空；不能接受客户端伪造此值。
	 * @param name 玩家公开角色名或“系统”，不暴露登录用户名。
	 * @param recipient 私聊/个人通知的收件角色编号；世界和全服公告为空。
	 * @param text 经格式校验的原文，浏览器仅通过转义插值展示。
	 * @param createdAt 持久化时间；以UTC瞬间传输，由浏览器转换显示时区。
	 * @param read 私聊表示真正收件人已读，系统表示当前查看者已读，不代表连接收到了提示。
	 */
	public record Message(String id, String channel, String sender, String name, String recipient, String text,
			Instant createdAt, boolean read) {
	}

	/** 历史按新到旧返回；next 是继续向更早消息翻页的排他游标，空值表示本页已到末尾。 */
	public record Page(List<Message> entries, String next) {
	}

	/** 仅返回双方公开角色资料与未读数量；最后消息全文仍通过历史接口鉴权读取。 */
	public record Conversation(String character, String name, long unread, String latest) {
	}

	/** 拒收陌生人仅影响后续私聊；黑名单有方向，不影响对方账号的其他游戏能力。 */
	public record Blocked(String character, String name) {
	}

	/**
	 * 本人会话摘要，不携带任何消息正文；战斗期间摘要也不可查询。
	 *
	 * @param character 当前认证账号对应的角色编号，作为面板与偏好隔离键。
	 * @param rejectStrangers 是否只接收已接受好友的后续私聊；不删除之前的陌生人历史。
	 * @param conversations 实际形成的私聊会话，按最近消息排序，不生成任意陌生人的预览。
	 * @param systemUnread 本人的个人通知与尚未过期公告未读数，不包含其他人的私有通知。
	 * @param blocked 本人主动建立的黑名单，公开名用于管理，不能查询被谁拉黑。
	 */
	public record Summary(String character, boolean rejectStrangers, List<Conversation> conversations,
			long systemUnread, List<Blocked> blocked) {
	}

	/**
	 * 玩家发送意图，任何字段都不是授权凭据；发件人必须从当前认证身份解析。
	 *
	 * @param requestId 标准UUID；同一成功内容重试返回原消息，修改内容则返回冲突。
	 * @param channel 只允许WORLD或PRIVATE，SYSTEM没有玩家写入口。
	 * @param peer 私聊精确角色名；世界频道忽略此值，不通过名字授权读取私有资料。
	 * @param text 玩家纯文本，服务端按码点限长、控制字符和共用禁词规则校验。
	 */
	public record Send(String requestId, String channel, String peer, String text) {
	}

	/** 已读确认仅在消息实际展示后发送；through 必须属于当前频道与参与者，不能拿其他私聊的编号推进。 */
	public record Read(String channel, String peer, String through) {
	}

	/** 角色级陌生人拒收开关，缺省为允许；设置写入保留原发送时间，不能用其重置限频。 */
	public record Settings(boolean rejectStrangers) {
	}

	/** 拉黑意图仅携带对端精确公开角色名；黑名单归属始终由服务端身份决定。 */
	public record Block(String name) {
	}

	/**
	 * 无正文的事务内更新事件，实时监听器仅在AFTER_COMMIT发送常量唤醒信号。
	 *
	 * @param sender 玩家消息的发件角色，系统为空；用于刷新发件人的已读显示或其他标签页。
	 * @param recipient 收件角色，为空表示公开更新；个人通知只唤醒本人，私聊唤醒双方。
	 */
	public record Changed(Long sender, Long recipient) {
	}

}
