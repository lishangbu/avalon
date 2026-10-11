package io.github.lishangbu.avalon.chat.domain;

import io.github.lishangbu.avalon.platform.BusinessException;
import java.util.UUID;

/** 与 HTTP、数据库无关的消息格式规则。内容检测仍由应用层调用共用禁词目录。 */
public final class ChatRules {

	private ChatRules() {
	}

	/** 首版仅允许玩家写世界与私聊；系统频道没有玩家发送入口。 */
	public static String channel(String value) {
		if (!"WORLD".equals(value) && !"PRIVATE".equals(value) && !"SYSTEM".equals(value))
			throw new BusinessException(400, "聊天频道无效");
		return value;
	}

	/** UUID 原样回执，拒绝任意超长请求键；失败输入不会消耗该请求号。 */
	public static void request(String value) {
		try {
			if (value == null || !UUID.fromString(value).toString().equals(value))
				throw new IllegalArgumentException();
		}
		catch (IllegalArgumentException failure) {
			throw new BusinessException(400, "聊天请求号无效");
		}
	}

	/** 按 Unicode 码点计数，补充平面汉字和表情各占一字；去除首尾空白但保留内部换行。 */
	public static String text(String value) {
		if (value == null)
			throw new BusinessException(400, "请输入消息");
		String text = value.strip();
		if (text.isBlank() || text.codePointCount(0, text.length()) > 500
				|| text.codePoints().anyMatch(c -> Character.isISOControl(c) && c != '\n' && c != '\t'))
			throw new BusinessException(400, "消息需为1～500字，不能包含控制字符");
		return text;
	}

}
