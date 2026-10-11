import {test} from '@playwright/test'
import {NaturalJourney} from './natural-journey'

const selected = process.env.AVALON_SIMULATION_ROUTE ?? 'story16'
if (!['story16', 'level15', 'both'].includes(selected)) throw new Error(`未知模拟路线：${selected}`)
for (const route of selected === 'both' ? ['story16', 'level15'] : [selected]) {
    test(`正式HTTP自然成长 ${route}`, async ({}, info) => {
        const journey = new NaturalJourney(process.env.AVALON_BASE_URL ?? 'http://localhost:8080')
        try {
            await journey.run(route === 'level15' ? 'level15' : 'story16')
        } finally {
            await info.attach('natural-simulation', {
                body: JSON.stringify(journey.report(), null, 2),
                contentType: 'application/json',
            })
        }
    })
}
