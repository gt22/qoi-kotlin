
internal fun Iterator<UByte>.readString(len: Int): String {
    val sb = StringBuilder(len)
    for (i in 0 until len) {
        sb.append(this.next().toInt().toChar())
    }
    return sb.toString()
}

internal fun Iterator<UByte>.readInt(): Int {
    var x = 0
    for (i in 0..3) {
        x = x shl 8
        x = x or (this.next().toInt() and 0xFF)
    }
    return x
}

internal suspend fun SequenceScope<UByte>.yieldInt(x: Int) {
    yield((x shr 24).toUByte())
    yield((x shr 16).toUByte())
    yield((x shr 8).toUByte())
    yield((x shr 0).toUByte())
}

internal suspend fun SequenceScope<UByte>.yieldString(s: String) {
    yieldAll(s.asSequence().map { it.code.toUByte() })
}

infix fun UByte.shl(n: Int): UByte = this.toInt().shl(n).toUByte()

infix fun UByte.shr(n: Int): UByte = this.toInt().shr(n).toUByte()