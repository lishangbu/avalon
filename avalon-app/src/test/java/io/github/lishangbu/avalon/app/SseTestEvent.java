package io.github.lishangbu.avalon.app;

import java.io.BufferedReader;
import java.io.EOFException;
import java.io.IOException;

/** 按 SSE 字段读取一个完整事件；仅移除冒号后的一个空格，不能 trim 掉真正的消息内容。 */
record SseTestEvent(String event, String data) {

	static SseTestEvent read(BufferedReader reader) throws IOException {
		String event = "";
		var data = new StringBuilder();
		boolean hasData = false;
		String line;
		while ((line = reader.readLine()) != null) {
			if (line.isEmpty()) {
				if (hasData)
					return new SseTestEvent(event.isEmpty() ? "message" : event, data.substring(0, data.length() - 1));
				event = "";
				continue;
			}
			if (line.startsWith(":"))
				continue;
			int colon = line.indexOf(':');
			String field = colon < 0 ? line : line.substring(0, colon);
			String value = colon < 0 ? "" : line.substring(colon + 1);
			if (value.startsWith(" "))
				value = value.substring(1);
			switch (field) {
				case "event" -> event = value;
				case "data" -> {
					data.append(value).append('\n');
					hasData = true;
				}
				default -> {
					// id、retry 和未知字段不属于当前事件的名称与正文。
				}
			}
		}
		throw new EOFException("SSE 流在完整事件的空行分隔符之前结束");
	}

}
