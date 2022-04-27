import BlockType.*

enum class BlockType(val tag: UByte) {
    QOI_OP_RGB(0b11111110u),
    QOI_OP_RGBA(0b11111111u),
    QOI_OP_INDEX(0b00000000u),
    QOI_OP_DIFF(0b01000000u),
    QOI_OP_LUMA(0b10000000u),
    QOI_OP_RUN(0b11000000u);

    companion object {

        val masked = arrayOf(QOI_OP_INDEX, QOI_OP_DIFF, QOI_OP_LUMA, QOI_OP_RUN)

        fun decode(tag: UByte): BlockType {
            if(tag == QOI_OP_RGB.tag) {
                return QOI_OP_RGB
            }
            if(tag == QOI_OP_RGBA.tag) {
                return QOI_OP_RGBA
            }
            val maskedTag = tag and 0b11000000u
            return masked.first { it.tag == maskedTag }
        }

    }
}

sealed class Block(var tag: UByte) {

    protected fun encodeInternal(encodeSpecific: suspend SequenceScope<UByte>.() -> Unit) = sequence {
        yield(tag)
        encodeSpecific()
    }

    open fun encode() = encodeInternal {  }

    companion object {

        fun decode(iter: Iterator<UByte>): Block {
            val tag = iter.next()
            val blockDecode: (UByte, Iterator<UByte>) -> Block = when(BlockType.decode(tag)) {
                QOI_OP_RGB -> RgbBlock.Companion::decode
                QOI_OP_RGBA -> RgbaBlock.Companion::decode
                QOI_OP_INDEX -> IndexBlock.Companion::decode
                QOI_OP_DIFF -> DiffBlock.Companion::decode
                QOI_OP_LUMA -> LumaBlock.Companion::decode
                QOI_OP_RUN -> RunBlock.Companion::decode
            }
            return blockDecode(tag, iter)
        }
    }

    class RgbBlock(var red: UByte, var green: UByte, var blue: UByte) : Block(QOI_OP_RGB.tag) {

        override fun encode() = encodeInternal {
            yield(red)
            yield(green)
            yield(blue)
        }

        companion object {

            fun decode(tag: UByte, iter: Iterator<UByte>) = RgbBlock(
                red = iter.next(),
                green = iter.next(),
                blue = iter.next()
            )
        }

    }

    class RgbaBlock(var red: UByte, var green: UByte, var blue: UByte, var alpha: UByte) : Block(QOI_OP_RGBA.tag) {

        override fun encode() = encodeInternal {
            yield(red)
            yield(green)
            yield(blue)
            yield(alpha)
        }

        companion object {

            fun decode(tag: UByte, iter: Iterator<UByte>) = RgbaBlock(
                red = iter.next(),
                green = iter.next(),
                blue = iter.next(),
                alpha = iter.next()
            )
        }

    }

    class IndexBlock(var index: UByte) : Block(QOI_OP_INDEX.tag or index) {

        init {
            require(index < 64u) { "Index must be less than 64" }
        }

        companion object {
            fun decode(tag: UByte, iter: Iterator<UByte>) = IndexBlock(tag and 0b00111111u)
        }

    }

    class DiffBlock private constructor(private var _dr: UByte, private var _dg: UByte, private var _db: UByte) : Block(
        QOI_OP_DIFF.tag
        or (_dr shl 4)
        or (_dg shl 2)
        or (_db shl 0)
    ) {

        val dr get() = (_dr.toInt() - 2)

        val dg get() = (_dg.toInt() - 2)

        val db get() = (_db.toInt() - 2)

        init {
            require(_dr < 4u) { "Red must be less than 4" }
            require(_dg < 4u) { "Green must be less than 4" }
            require(_db < 4u) { "Blue must be less than 4" }
        }

        companion object {

            fun fromValues(dr: Int, dg: Int, db: Int) = DiffBlock(
                _dr = (dr + 2).toUByte(),
                _dg = (dg + 2).toUByte(),
                _db = (db + 2).toUByte()
            )

            fun decode(tag: UByte, iter: Iterator<UByte>) = DiffBlock(
                _dr = (tag shr 4) and 0b00000011u,
                _dg = (tag shr 2) and 0b00000011u,
                _db = (tag shr 0) and 0b00000011u
            )
        }
    }

    class LumaBlock private constructor(private var _dg: UByte = 0u, private var _drdg: UByte = 0u, private var _dbdg: UByte = 0u) : Block(
        QOI_OP_LUMA.tag or _dg
    ) {

        val dg get() = (_dg.toInt() - 32)

        val dr get() = (_drdg.toInt() - 8 + dg)

        val db get() = (_dbdg.toInt() - 8 + dg)

        init {
            require(_dg < 64u) { "Green must be less than 64" }
            require(_drdg < 16u) { "Red-Green must be less than 16" }
            require(_dbdg < 16u) { "Blue-Green must be less than 16" }
        }

        override fun encode() = encodeInternal {
            yield((_drdg shl 4) or _dbdg)
        }

        companion object {

            fun fromValues(dr: Int, dg: Int, db: Int) = LumaBlock(
                _dg = (dg + 32).toUByte(),
                _drdg = (dr - dg + 8).toUByte(),
                _dbdg = (db - dg + 8).toUByte()
            )

            fun decode(tag: UByte, iter: Iterator<UByte>): LumaBlock {
                val dd = iter.next();
                return LumaBlock(
                    _dg = tag and 0b00111111u,
                    _drdg = dd shr 4,
                    _dbdg = dd and 0b00001111u
                )
            }
        }
    }

    class RunBlock private constructor(private var _run: UByte) : Block(QOI_OP_RUN.tag or _run) {

        val run get() = _run.toInt() + 1

        init {
            require(_run < 63u) { "Run must be less than 63" }
        }

        companion object {

            fun fromValues(run: Int) = RunBlock((run - 1).toUByte())

            fun decode(tag: UByte, iter: Iterator<UByte>) = RunBlock(tag and 0b00111111u)
        }

    }

}
