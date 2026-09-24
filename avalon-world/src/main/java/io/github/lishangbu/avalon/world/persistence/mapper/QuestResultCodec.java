package io.github.lishangbu.avalon.world.persistence.mapper;

import tools.jackson.databind.ObjectMapper;

import io.github.lishangbu.avalon.world.application.model.QuestResult;

import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;

/**
 * 任务重放结果的存储编码适配器。结果与奖励在同一数据库事务内保存。 这里的 JSON 是已执行命令的回执，既不是内容版本，也不是热更新快照。
 * 解码失败必须报错，不能重新执行任务以“修复”回执，否则会重复发放奖励。
 */
@Component
@RequiredArgsConstructor
public class QuestResultCodec {

	private final ObjectMapper json;

	/**
	 * 编码失败会以运行时异常传播，使调用方奖励事务整体回滚。
	 */
	public String encode(QuestResult result) {
		return json.writeValueAsString(result);
	}

	/**
	 * 只恢复首次执行的回复；不读取当前奖励配置，避免重试时文本随配置改变。
	 */
	public QuestResult decode(String response) {
		return json.readValue(response, QuestResult.class);
	}

}
