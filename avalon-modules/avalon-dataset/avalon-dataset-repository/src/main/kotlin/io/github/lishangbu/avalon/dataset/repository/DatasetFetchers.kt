package io.github.lishangbu.avalon.dataset.repository

import io.github.lishangbu.avalon.dataset.entity.*
import org.babyfish.jimmer.sql.kt.fetcher.newFetcher

/** 数据集模块常用对象抓取器 */
internal object DatasetFetchers {
    /** 树果及其关联基础信息抓取器 */
    val BERRY_WITH_ASSOCIATIONS =
        newFetcher(Berry::class).`by` {
            allScalarFields()
            berryFirmness {
                internalName()
                name()
            }
            naturalGiftType {
                internalName()
                name()
            }
        }

    /** 性格及其关联基础信息抓取器 */
    val NATURE_WITH_ASSOCIATIONS =
        newFetcher(Nature::class).`by` {
            allScalarFields()
            decreasedStat {
                internalName()
                name()
            }
            increasedStat {
                internalName()
                name()
            }
            hatesBerryFlavor {
                internalName()
                name()
            }
            likesBerryFlavor {
                internalName()
                name()
            }
        }
}
