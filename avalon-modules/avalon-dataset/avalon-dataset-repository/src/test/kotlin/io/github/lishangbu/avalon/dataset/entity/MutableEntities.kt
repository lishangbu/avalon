@file:Suppress("FunctionName", "ktlint:standard:function-naming")

package io.github.lishangbu.avalon.dataset.entity

class MutableBerry : Berry {
    override var id: Long = 0
    override var internalName: String? = null
    override var name: String? = null
    override var growthTime: Int? = null
    override var maxHarvest: Int? = null
    override var bulk: Int? = null
    override var smoothness: Int? = null
    override var soilDryness: Int? = null
    override var berryFirmness: BerryFirmness? = null
    override var naturalGiftType: Type? = null
    override var naturalGiftPower: Int? = null
}

class MutableBerryFirmness : BerryFirmness {
    override var id: Long = 0
    override var internalName: String? = null
    override var name: String? = null
}

class MutableBerryFlavor : BerryFlavor {
    override var id: Long = 0
    override var internalName: String? = null
    override var name: String? = null
}

class MutableMoveDamageClass : MoveDamageClass {
    override var id: Long = 0
    override var internalName: String? = null
    override var name: String? = null
    override var description: String? = null
}

class MutableStat : Stat {
    override var id: Long = 0
    override var internalName: String? = null
    override var name: String? = null
    override var gameIndex: Int? = null
    override var battleOnly: Boolean? = null
    override var readonly: Boolean = false
    override var moveDamageClass: MoveDamageClass? = null
}

class MutableType : Type {
    override var id: Long = 0
    override var internalName: String? = null
    override var name: String? = null
}

class MutableTypeEffectivenessEntryId : TypeEffectivenessEntryId {
    override var attackingTypeId: Long = 0
    override var defendingTypeId: Long = 0
}

class MutableTypeEffectivenessEntry : TypeEffectivenessEntry {
    override var id: TypeEffectivenessEntryId = MutableTypeEffectivenessEntryId()
    override var multiplierPercent: Int? = null
}

fun Berry(): MutableBerry = MutableBerry()

fun BerryFirmness(): MutableBerryFirmness = MutableBerryFirmness()

fun BerryFlavor(): MutableBerryFlavor = MutableBerryFlavor()

fun MoveDamageClass(): MutableMoveDamageClass = MutableMoveDamageClass()

fun Stat(): MutableStat = MutableStat()

fun Type(): MutableType = MutableType()

fun TypeEffectivenessEntryId(): MutableTypeEffectivenessEntryId = MutableTypeEffectivenessEntryId()

fun TypeEffectivenessEntry(): MutableTypeEffectivenessEntry = MutableTypeEffectivenessEntry()
