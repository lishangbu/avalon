import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

// 各入口只改变验证范围，不改变经验、随机概率、采集时钟或生产规则。
// integration 使用阶段夹具覆盖采集、供料、库存和 E009 交付。
const suites = {
  // 结算边界改动优先运行四种批次场景，包含单件对照、升级收缩和工具报废。
  batch: 'ManufacturingSupplyFlowTest#manufacturingBatchPreservesSettlementBoundaries',
  // 库存整理及正式丢弃改动只运行库存套件，不重复执行高阶制造和采集。
  inventory: 'ManufacturingInventoryTest',
  // 高阶跨专业供料、资源恢复和工具补造使用完整供料套件。
  supply: 'ManufacturingSupplyFlowTest',
  quick: 'ManufacturingSupplyFlowTest,ManufacturingInventoryTest',
  integration: 'GatheringFlowTest,ManufacturingSupplyFlowTest,ManufacturingInventoryTest,LifeCraftingFlowTest#trialBlueprintCraftsCompleteArmorAndOnlyFreshProductionCompletesCommission',
};
const [mode, ...options] = process.argv.slice(2);
// 仅接受固定选项，避免拼写错误悄悄选择错误的验证范围。
if (!Object.hasOwn(suites, mode ?? '') || options.some((option) => option !== '--dry-run')) {
  console.error('用法：node scripts/test-manufacturing.mjs <batch|inventory|supply|quick|integration> [--dry-run]');
  process.exit(2);
}

// 从脚本位置定位仓库，使其他目录调用时也使用本仓库的 Maven Wrapper。
const cwd = fileURLToPath(new URL('../', import.meta.url));
const wrapper = process.platform === 'win32' ? '.\\mvnw.cmd' : './mvnw';
const args = [
  '--batch-mode',
  '--no-transfer-progress',
  '-pl', 'avalon-app', '-am',
  '-Dskip.frontend=true',
  `-Dtest=${suites[mode]}`,
  '-Dsurefire.failIfNoSpecifiedTests=false',
  'test',
];
console.log(mode === 'integration'
    ? '制造集成回归：使用阶段夹具验证采集、供料、库存及 E009 制造交付。'
  : `局部制造回归（${mode}）：通过不能证明完整自然路线可达。`);
console.log([wrapper, ...args].join(' '));

// 预览不启动 Maven，也不生成测试报告。
if (!options.includes('--dry-run')) {
  // Windows 的 Wrapper 是批处理文件，必须通过 cmd 执行；所有参数均来自上面的固定列表。
  // Java 版本沿用调用者环境，本地可用 mise exec -- node ... 选择项目工具链。
  const result = process.platform === 'win32'
    ? spawnSync('cmd.exe', ['/d', '/s', '/c', [wrapper, ...args].join(' ')], { cwd, stdio: 'inherit' })
    : spawnSync(wrapper, args, { cwd, stdio: 'inherit' });
  // 保留 Maven 的失败退出码；启动失败或信号终止也不能被入口误报为通过。
  if (result.error) console.error(result.error.message);
  process.exitCode = result.status ?? 1;
}
