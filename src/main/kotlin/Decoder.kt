import java.awt.image.BufferedImage
import java.nio.file.Path
import kotlin.io.path.inputStream

object QoiDecoder {

    context(QoiState) private fun pixel(p: Pixel): Array<Pixel> {
        updateIndexPixel(p)
        updateLastPixel(p)
        return arrayOf(p)
    }

    private infix fun UByte.diff(d: Int) = ((this.toInt() + d) % 256).toUByte()
    private fun Pixel.diff(dr: Int, dg: Int, db: Int) =
        Pixel(
            r diff dr,
            g diff dg,
            b diff db,
            a
        )

    context(QoiState) fun Block.toPixels(): Array<Pixel> = when (this) {
        is Block.RgbBlock -> pixel(Pixel(red, green, blue, lastPixel.a))
        is Block.RgbaBlock -> pixel(Pixel(red, green, blue, alpha))
        is Block.IndexBlock -> pixel(getPixel(index.toInt()))
        is Block.DiffBlock -> pixel(lastPixel.diff(dr, dg, db))
        is Block.LumaBlock -> pixel(lastPixel.diff(dr, dg, db))
        is Block.RunBlock -> Array(run) { lastPixel } // WARNING: Doesn't update state. It so happens that run can't cause any change to the state, but be careful
    }

    private fun decodeBlocks(header: Header, iter: Iterator<UByte>): Sequence<Pixel> = sequence {
        var pixelsLeft = header.width * header.height
        with(QoiState()) {
            while (pixelsLeft > 0) {
                val block = Block.decode(iter)
                val pixels = block.toPixels()
                yieldAll(pixels.iterator())
                pixelsLeft -= pixels.size
            }
        }
    }

    private fun confirmEos(iter: Iterator<UByte>) {
        repeat(7) {
            check(iter.next() == 0u.toUByte()) { "Incorrect End of Stream" }
        }
        check(iter.next() == 1u.toUByte()) { "Incorrect End of Stream" }
    }

    fun decode(iter: Iterator<UByte>, enforceEoS: Boolean = false): Pair<Header, List<Pixel>> {
        val header = Header.decode(iter)
        val pixels = decodeBlocks(header, iter).toList()
        if(enforceEoS) confirmEos(iter)
        return header to pixels
    }

    fun decode(p: Path, enforceEoS: Boolean = false): Pair<Header, List<Pixel>> {
        p.inputStream().use { i ->
            return decode(sequence {
                while (true) {
                    val b = i.read()
                    if (b == -1) break
                    yield(b.toUByte())
                }
            }.iterator(), enforceEoS)
        }
    }
}

fun Pair<Header, List<Pixel>>.toBufferedImage(): BufferedImage {
    val (header, pixels) = this
    return BufferedImage(header.width, header.height, BufferedImage.TYPE_INT_ARGB).apply {
        this.raster.setPixels(0, 0, this.width, this.height, pixels.flatMap { it.toARGBSeq() }.toIntArray())
    }
}