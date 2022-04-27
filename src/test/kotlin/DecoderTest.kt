import java.io.File
import java.nio.file.Path
import javax.imageio.ImageIO
import kotlin.test.Test
import kotlin.test.assertTrue

class QoiTest {

    fun testDecode(qoi: Path, png: Path) {
        val decoded = QoiDecoder.decode(qoi, enforceEoS = true).toBufferedImage()
        val original = ImageIO.read(png.toFile())

        assertTrue(compareImages(original, decoded), "Error on ${qoi.fileName}/${png.fileName}")
    }

    @Test
    fun testDecoder() {
        forAllFiles(Path.of("./qoi_test_images")) { qoi, png ->
            testDecode(qoi, png)
        }
    }

    fun testEncode(qoi: Path, png: Path) {
        val encoded = QoiEncoder.encode(png)
        // TODO: Compare to qoi original without exact match
        val img = QoiDecoder.decode(encoded.iterator()).toBufferedImage()
        val original = ImageIO.read(png.toFile())

        assertTrue(compareImages(original, img), "Error on ${qoi.fileName}/${png.fileName}")
    }

    @Test
    fun testEncoder() {
        forAllFiles(Path.of("./qoi_test_images")) { qoi, png ->
            testEncode(qoi, png)
        }
    }
}