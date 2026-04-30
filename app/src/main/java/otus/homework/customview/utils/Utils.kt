package otus.homework.customview.utils

fun Float.deleteNegativity() =
    if (this < 0f) 0f else this

fun <T>T.applyIf(condition: Boolean, block: T.() -> Unit) : T{
    return if (condition) this.apply { block() } else this
}

fun Float.normalizeDegrees() =
    if (this < -90) this + 450 else this + 90