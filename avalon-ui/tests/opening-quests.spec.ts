import {test} from '@playwright/test'
import {completeNaturalChapter} from './support/natural-chapter'

/** 独立保留整章验收；三人联机复用同一页面成长路径。 */
test('BQ001至BQ030自然主线与15级成长闭环', async ({page}, testInfo) => {
    test.setTimeout(14_400_000)
    await completeNaturalChapter(page, testInfo)
})
