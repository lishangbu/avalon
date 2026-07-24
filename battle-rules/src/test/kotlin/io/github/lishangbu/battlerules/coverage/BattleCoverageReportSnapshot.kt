package io.github.lishangbu.battlerules.coverage

import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.assertEquals

/** 统一覆盖报告的生成写入与仓库快照防漂移校验。 */
class BattleCoverageReportSnapshot(
	private val projectRoot: Path,
) {
	/** 根据生成开关写入报告，否则断言仓库中的报告仍与运行时数据一致。 */
	fun verifyOrWrite(
		reportPath: String,
		report: String,
		writeProperty: String,
		staleMessage: String,
	) {
		val resolvedReportPath = projectRoot.resolve(reportPath)
		if (System.getProperty(writeProperty).toBoolean()) {
			Files.createDirectories(resolvedReportPath.parent)
			Files.writeString(resolvedReportPath, report)
		} else {
			assertEquals(Files.readString(resolvedReportPath), report, staleMessage)
		}
	}
}
