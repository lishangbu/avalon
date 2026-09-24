import {test} from '@playwright/test'
import {completeFormalOpening} from './support/formal-opening'

test.use({actionTimeout: 15_000})

test('BQ001至BQ030正式开篇与返乡闭环', async ({page}, testInfo) => {
    test.setTimeout(900_000)
    await completeFormalOpening(page, testInfo)
})
