import java.awt.image.BufferedImage
import java.io.File
import java.nio.file.Path

fun compareImages(imgA: BufferedImage, imgB: BufferedImage): Boolean {
    // The images must be the same size.
    if (imgA.width != imgB.width || imgA.height != imgB.height) {
        return false
    }
    val width = imgA.width
    val height = imgA.height

    // Loop over every pixel.
    for (y in 0 until height) {
        for (x in 0 until width) {
            // Compare the pixels for equality.
            if (imgA.getRGB(x, y) != imgB.getRGB(x, y)) {
                return false
            }
        }
    }
    return true
}

fun forAllFiles(from: Path, act: (Path, Path) -> Unit) {
    from.toFile().listFiles().forEach {
        if (it.extension == "qoi") {
            val png = it.toPath().resolveSibling(it.nameWithoutExtension + ".png")
            act(it.toPath(), png)
        }
    }
}