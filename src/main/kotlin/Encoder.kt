import QoiDecoder.toPixels
import java.nio.file.Path
import javax.imageio.ImageIO

object QoiEncoder {

    private suspend fun SequenceScope<UByte>.yield(block: Block) = yieldAll(block.encode())

    private suspend fun SequenceScope<UByte>.yieldValidate(last: Pixel, cache: MutableList<Pixel>, target: Pixel, block: Block) {
        yield(block)
        with(QoiState(cache, last)) {
            val pixels = block.toPixels()
            if(pixels.last() != target) {
                throw IllegalStateException("Invalid block: $block")
            }
        }
    }

    private fun encodePixels(iter: Iterator<Pixel>): Sequence<UByte> = sequence {
        with(QoiState()) {
            var currentRun = 0
            for (p in iter) {
                val last = lastPixel
                val cacheCopy = MutableList(cache.size) { cache[it] }
                val samePixel = updateLastPixel(p)
                if (samePixel) {
                    currentRun++
                }
                if (currentRun > 0 && (!samePixel || currentRun == 62)) {
                    yieldValidate(last, cacheCopy, last, Block.RunBlock.fromValues(currentRun))
                    currentRun = 0
                }
                assert(currentRun < 62)
                if (!samePixel) {
                    assert(currentRun == 0)
                    val index = updateIndexPixel(p)
                    if (index != -1) {
                        yieldValidate(last, cacheCopy, p, Block.IndexBlock(index.toUByte()))
                    } else {
                        if (p.a != last.a) {
                            yieldValidate(last, cacheCopy, p, Block.RgbaBlock(p.r, p.g, p.b, p.a))
                        } else {
                            val dr = p.r.toByte() - last.r.toByte()
                            val dg = p.g.toByte() - last.g.toByte()
                            val db = p.b.toByte() - last.b.toByte()

                            if (dr in -2..1 && dg in -2..1 && db in -2..1) {
                                yieldValidate(last, cacheCopy, p, Block.DiffBlock.fromValues(dr, dg, db))
                            } else {
                                if ((dr - dg) in -8..7 && dg in -32..31 && (db - dg) in -8..7) {
                                    val block = Block.LumaBlock.fromValues(dr = dr, dg = dg, db = db)
                                    try {
                                        yieldValidate(last, cacheCopy, p, block)
                                    } catch(e: IllegalStateException) {
                                        println("$block, $p, $last, $dr, $dg, $db")
                                        e.printStackTrace()
                                    }
                                } else {
                                    yieldValidate(last, cacheCopy, p, Block.RgbaBlock(p.r, p.g, p.b, p.a))
                                }
                            }
                        }
                    }
                }
            }
            if (currentRun > 0) {
                yieldAll(Block.RunBlock.fromValues(currentRun).encode())
            }
        }
    }

    fun encode(
        width: Int,
        height: Int,
        iter: Iterator<Pixel>,
        channels: Channels = Channels.RGBA,
        colorspace: Colorspace = Colorspace.SRGB
    ): Sequence<UByte> {
        val header = Header(
            width,
            height,
            channels,
            colorspace
        )
        val zero: UByte = 0u
        val one: UByte = 1u
        val eos = sequence {
            repeat(7) { yield(zero) }
            yield(one)
        }
        return header.encode() + encodePixels(iter) + eos
    }

    fun encode(p: Path): Sequence<UByte> {
        val png = ImageIO.read(p.toFile())
        val width = png.width
        val height = png.height
        return encode(width, height, sequence {
            for(y in 0 until height) {
                for(x in 0 until width) {
                    yield(Pixel.fromARGB(png.getRGB(x, y)))
                }
            }
        }.iterator())
    }

}