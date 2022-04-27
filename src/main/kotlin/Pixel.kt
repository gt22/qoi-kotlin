data class Pixel(
    val r: UByte,
    val g: UByte,
    val b: UByte,
    val a: UByte = 255.toUByte()
) {

    fun toARGB() = (a.toInt() shl 24) or (r.toInt() shl 16) or (g.toInt() shl 8) or b.toInt()

    fun toARGBSeq() = sequenceOf(r.toInt(), g.toInt(), b.toInt(), a.toInt())

    companion object {
        fun fromARGB(argb: Int) = Pixel(
            a = ((argb shr 24) and 0xFF).toUByte(),
            r = ((argb shr 16) and 0xFF).toUByte(),
            g = ((argb shr 8) and 0xFF).toUByte(),
            b = ((argb shr 0) and 0xFF).toUByte()
        )
    }
}
