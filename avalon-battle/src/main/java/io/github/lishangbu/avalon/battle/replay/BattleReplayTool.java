package io.github.lishangbu.avalon.battle.replay;

import java.nio.file.Path;

/** 独立命令行入口；当前提交的运行类路径即可执行，不启动服务及数据库。 */
public final class BattleReplayTool {

	private BattleReplayTool() {
	}

	/** validate校验完整规则闭包，replay核对指定JSONLines文件；失败向外抛出并以非零状态退出。 */
	public static void main(String[] arguments) throws Exception {
		if (arguments.length == 1 && arguments[0].equals("validate")) {
			BattleReplayRules.assemble(BattleReplayRules.capture());
			System.out.println("战斗配置与扩展注册校验通过。");
			return;
		}
		if (arguments.length == 2 && arguments[0].equals("replay")) {
			int rounds = BattleReplayVerifier.verify(Path.of(arguments[1]));
			System.out.println("战斗重放核验通过，共 " + rounds + " 个完整回合。");
			return;
		}
		throw new IllegalArgumentException("用法：BattleReplayTool validate | replay <JSONLines文件>");
	}

}
