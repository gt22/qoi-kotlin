

data class Header(
        var width: Int,
        var height: Int,
        var channels: Channels,
        var colorspace: Colorspace
) {
    fun encode() = sequence {
        yieldString("qoif")
        yieldInt(width)
        yieldInt(height)
        yield(channels.value)
        yield(colorspace.value)
    }

    companion object {
        fun decode(iterator: Iterator<UByte>): Header {
            val magic = iterator.readString(4)
            if (magic != "qoif") throw IllegalArgumentException("Invalid magic")
            val width = iterator.readInt()
            val height = iterator.readInt()
            val channels = Channels.decode(iterator)
            val colorspace = Colorspace.decode(iterator)
            return Header(width, height, channels, colorspace)
        }
    }

}

enum class Channels(val value: UByte) {
    RGB(3u),
    RGBA(4u);

    companion object {
        fun decode(iter: Iterator<UByte>): Channels {
            val value = iter.next()
            return values().first { it.value == value }
        }
    }
}

enum class Colorspace(val value: UByte) {
    SRGB(0u),
    Linear(1u);

    companion object {
        fun decode(iter: Iterator<UByte>): Colorspace {
            val value = iter.next()
            return values().first { it.value == value }
        }
    }
}