package io.github.lishangbu.avalon.battle.replay;

import io.github.lishangbu.avalon.battle.domain.BattleState;
import io.github.lishangbu.avalon.battle.domain.BattleRoundResult;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/** 事务提交后写文本战斗记录；文件失败明确告警，但不能让已经提交的资产响应表现为回滚。 */
@Component
public class BattleReplayJournal {

	/** 文件失败记录在普通文本运行日志中，不创建数据库日志表或补偿任务。 */
	private static final Logger LOG = LoggerFactory.getLogger(BattleReplayJournal.class);

	/** 独立文本存储，不保留仓库或库存服务。 */
	private final BattleReplayFiles files;

	/** 仅结束记录适用保留期限，活动记录不受此配置影响。 */
	private final Duration retention;

	/** 配置写具体值，默认logs/battles与30天；目录由生产容器挂载持久化。 */
	public BattleReplayJournal(@Value("${avalon.battle.logs.directory:logs/battles}") String directory,
			@Value("${avalon.battle.logs.retention-days:30}") int days) {
		if (days < 1)
			throw new IllegalArgumentException("战斗文本日志保留天数必须为正");
		files = new BattleReplayFiles(Path.of(directory));
		retention = Duration.ofDays(days);
	}

	/** 先序列化初始状态，防止同事务后续变更改写提交后才落盘的开局证据。 */
	public void start(String mode, long battle, BattleState initial) {
		var frozen = freeze(initial, BattleState.class);
		Instant now = Instant.now();
		afterCommit(mode, battle, () -> files.start(mode, battle, frozen, now));
	}

	/** 固定纯结算输入和输出；重复请求返回回执时不调用此方法，避免追加相同回合。 */
	public void round(String mode, long battle, String request, BattleState before, BattleRoundResult result,
			String committedStatus) {
		var frozenBefore = freeze(before, BattleState.class);
		var frozenResult = freeze(result, BattleRoundResult.class);
		Instant now = Instant.now();
		afterCommit(mode, battle, () -> {
			files.append(mode, battle, request, frozenBefore, frozenResult);
			if (!committedStatus.equals("ACTIVE"))
				files.finish(mode, battle, committedStatus, now);
		});
	}

	/** 无动作终局仍需显式关闭记录，不能等待清理任务根据状态猜测结束。 */
	public void finish(String mode, long battle, String status) {
		Instant now = Instant.now();
		afterCommit(mode, battle, () -> files.finish(mode, battle, status, now));
	}

	/** 回合外的正式状态变更也在提交后追加，保持前后快照独立于后续事务修改。 */
	public void state(String mode, long battle, String reason, BattleState before, BattleState after) {
		var frozenBefore = freeze(before, BattleState.class);
		var frozenAfter = freeze(after, BattleState.class);
		if (BattleReplayCodec.write(frozenBefore).equals(BattleReplayCodec.write(frozenAfter)))
			return;
		afterCommit(mode, battle, () -> files.state(mode, battle, reason, frozenBefore, frozenAfter));
	}

	/** 由宿主维护调度周期唤醒，文件失败只告警而不停止其他维护任务。 */
	public void cleanup() {
		try {
			int removed = files.cleanup(Instant.now(), retention);
			if (removed > 0)
				LOG.info("已清理{}份过期的已结束战斗文本记录", removed);
		}
		catch (Exception exception) {
			LOG.warn("战斗文本记录清理失败，文件保持原样", exception);
		}
	}

	/** 序列化采用日志严格格式，副本不会和提交后的宿主状态共享可变集合。 */
	private <T> T freeze(T value, Class<T> type) {
		return BattleReplayCodec.read(BattleReplayCodec.write(value), type);
	}

	/** 必须在正式事务注册，回滚不落盘；afterCommit内的异常不向玩家请求传播。 */
	private void afterCommit(String mode, long battle, FileAction action) {
		if (!TransactionSynchronizationManager.isActualTransactionActive()
				|| !TransactionSynchronizationManager.isSynchronizationActive())
			throw new IllegalStateException("战斗记录只能在正式事务内登记");
		TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
			@Override
			public void afterCommit() {
				try {
					action.run();
				}
				catch (Exception exception) {
					LOG.warn("战斗{}:{}已提交，但文本记录写入失败；缺失记录不可精确重放", mode, battle, exception);
				}
			}
		});
	}

	/** 文件动作只携带已冻结数据，checked异常在提交后统一处理。 */
	@FunctionalInterface
	private interface FileAction {

		void run() throws Exception;

	}

}
