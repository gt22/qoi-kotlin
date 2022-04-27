
data class QoiState(
    val cache: MutableList<Pixel> = MutableList(64) { Pixel(0u, 0u, 0u, 0u) },
    private var lastPixel_: Pixel = Pixel(0u, 0u, 0u, 255u)
) {

    private fun Pixel.hash() = ((r * 3u + g * 5u + b * 7u + a * 11u) % 64u).toInt()

    fun updateIndexPixel(p: Pixel): Int = if (cache[p.hash()] == p) {
        p.hash()
    } else {
        cache[p.hash()] = p
        -1
    }

    fun updateLastPixel(p: Pixel): Boolean = if (p != lastPixel) {
        lastPixel_ = p
        false
    } else {
        true
    }

    val lastPixel: Pixel
        get() = lastPixel_

    fun getPixel(index: Int) = cache[index]

}