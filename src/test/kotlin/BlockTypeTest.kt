import org.junit.Test
import kotlin.test.assertEquals

class BlockTypeTest {


    @Test
    fun decodeRGB() {
        assertEquals(BlockType.QOI_OP_RGB, BlockType.decode(0b11111110u))
    }

    @Test
    fun decodeRGBA() {
        assertEquals(BlockType.QOI_OP_RGBA, BlockType.decode(0b11111111u))
    }

    @Test
    fun decodeIndex() {
        for (i in 0u..63u) {
            assertEquals(BlockType.QOI_OP_INDEX, BlockType.decode(i.toUByte()))
        }
    }

    @Test
    fun decodeDiff() {
        for (dr in -2..1) {
            for (dg in -2..1) {
                for (db in -2..1) {
                    val r = (dr + 2).toUByte()
                    val g = (dg + 2).toUByte()
                    val b = (db + 2).toUByte()
                    val i = (r shl 4) or (g shl 2) or (b shl 0)
                    val mask: UByte = 0b01000000u
                    assertEquals(BlockType.QOI_OP_DIFF, BlockType.decode(mask or i))
                }
            }
        }
    }

    @Test
    fun decodeLuma() {
        for (dg in -32..31) {
            val g = (dg + 32).toUByte()
            val mask: UByte = 0b10000000u
            assertEquals(BlockType.QOI_OP_LUMA, BlockType.decode(mask or g))
        }
    }

    @Test
    fun decodeRun() {
        for(run in 1..62) {
            val mask: UByte = 0b11000000u
            assertEquals(BlockType.QOI_OP_RUN, BlockType.decode(mask or (run - 1).toUByte()))
        }
    }

}