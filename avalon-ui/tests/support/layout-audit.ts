import {expect, type Page, type TestInfo} from '@playwright/test'

/** 在真实业务状态上检查视口和系统主题；地图画布允许内部横向滚动，页面本身不允许。 */
export async function auditLayout(page: Page, info: TestInfo, name: string) {
    if (process.env.AVALON_LAYOUT_AUDIT !== '1' || info.project.name !== 'mobile') return
    const original = page.viewportSize()!
    try {
        for (const colorScheme of ['light', 'dark'] as const) {
            await page.emulateMedia({colorScheme})
            await expect(page.locator('html')).toHaveAttribute('data-theme', colorScheme)
            for (const viewport of [
                {width: 320, height: 640},
                {width: 844, height: 390},
            ]) {
                await page.setViewportSize(viewport)
                await expect
                    .poll(
                        () =>
                            page.evaluate(async () => {
                                const failures: string[] = []
                                if (document.documentElement.scrollWidth > innerWidth) failures.push('document')
                                for (const content of document.querySelectorAll('ion-content')) {
                                    if (!content.checkVisibility()) continue
                                    const scroll = await content.getScrollElement()
                                    if (scroll.scrollWidth > scroll.clientWidth + 1) failures.push('ion-content')
                                }
                                // 只检查最上层弹层的固定操作区，底层弹层被覆盖属于正常行为。
                                const overlays = [...document.querySelectorAll('ion-modal')].filter((node) =>
                                    node.checkVisibility(),
                                )
                                const surface =
                                    overlays.at(-1) ??
                                    [...document.querySelectorAll('div.ion-page')].find((node) =>
                                        node.checkVisibility(),
                                    )
                                for (const button of surface?.querySelectorAll(
                                    'ion-footer ion-button, ion-footer nav button',
                                ) ?? []) {
                                    const bounds = button.getBoundingClientRect()
                                    if (
                                        bounds.left < 0 ||
                                        bounds.right > innerWidth + 1 ||
                                        bounds.top < 0 ||
                                        bounds.bottom > innerHeight + 1
                                    ) {
                                        failures.push(`footer:${button.textContent?.trim()}`)
                                    }
                                }
                                return failures
                            }),
                        {message: `${name} ${colorScheme} ${viewport.width} 不应横向溢出`},
                    )
                    .toEqual([])
                // Sheet 手势持有暂停的 Web Animation；截图不能强制完成它，否则会改变弹层位置。
                await page.screenshot({path: info.outputPath(`layout-${name}-${colorScheme}-${viewport.width}.png`)})
            }
        }
    } finally {
        await page.setViewportSize(original)
        await page.emulateMedia({colorScheme: 'light'})
    }
}
