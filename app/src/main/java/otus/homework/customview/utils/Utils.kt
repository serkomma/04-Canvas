package otus.homework.customview.utils

fun <T> List<T>.preLast() = this[this.size - 2]

fun <T>T.applyIf(condition: Boolean, block: T.() -> Unit) : T{
    return if (condition) this.apply { block() } else this
}

fun Float.normalizeDegrees() =
    if (this < -90) this + 450 else this + 90