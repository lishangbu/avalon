import { spawnSync } from 'node:child_process';
import { fileURLToPath } from 'node:url';

// 两种入口只改变验证范围，不改变经验、随机概率、采集时钟或生产规则。
// quick 用于制造助手的局部回归；full 才执行四角色从开局到 E009 的自然路线。
const suites = {
  quick: 'NaturalEndgameCraftingFlowTest,NaturalManufacturingInventoryTest',
  full: 'NaturalExtendedStoryTest#naturalSuppliersReachIronToolQualification',
};
const [mode, ...options] = process.argv.slice(2);
// 仅接受固定选项，避免拼写错误悄悄选择错误的验证范围。
if (!Object.hasOwn(suites, mode ?? '') || options.some((option) => option !== '--dry-run')) {
  console.error('用法：node scripts/test-manufacturing.mjs <quick|full> [--dry-run]');
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
console.log(mode === 'quick'
  ? '局部制造回归：通过不能证明完整自然路线可达。'
  : '完整自然制造验收：保留实际成长、地图资格、供料、工具损耗及 E009 交付。');
console.log([wrapper, ...args].join(' '));

// 预览不启动 Maven，也不触碰当前正在执行的长路线及其测试报告。
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
