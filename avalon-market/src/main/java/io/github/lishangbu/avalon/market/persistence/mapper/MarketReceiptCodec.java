package io.github.lishangbu.avalon.market.persistence.mapper;

import tools.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

/** 回执编码属于持久化边界；两侧从文本解析，避免同值long/int被不同JSON节点误判。 */
@Component
@RequiredArgsConstructor
public class MarketReceiptCodec {

	private final ObjectMapper json;

	public String encode(Object value) {
		return json.writeValueAsString(value);
	}

	public boolean matches(String stored, Object value) {
		return json.readTree(stored).equals(json.readTree(encode(value)));
	}

	public <T> T decode(String stored, Class<T> type) {
		return json.readValue(stored, type);
	}

}
