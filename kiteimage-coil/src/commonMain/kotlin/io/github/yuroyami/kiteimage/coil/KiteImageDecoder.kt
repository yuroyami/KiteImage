package io.github.yuroyami.kiteimage.coil

import coil3.ImageLoader
import coil3.decode.DecodeResult
import coil3.decode.Decoder
import coil3.decode.ImageSource
import coil3.fetch.SourceFetchResult
import coil3.request.Options
import io.github.yuroyami.kiteimage.ImageFormat
import io.github.yuroyami.kiteimage.KiteImage
import okio.use

/**
 * A [coil3.decode.Decoder] backed by KiteImage's pure-Kotlin codecs.
 *
 * Register the [Factory] on your ImageLoader and Coil keeps doing everything it
 * is good at — network fetch, disk + memory cache, request lifecycle — while
 * decoding runs through KiteImage with identical behavior on every target:
 *
 * ```kotlin
 * ImageLoader.Builder(context)
 *     .components { add(KiteImageDecoder.Factory()) }
 *     .build()
 * ```
 *
 * The factory claims only inputs KiteImage fully decodes (GIF, and the PNG/BMP
 * feature subsets) and declines everything else — including interlaced/CgBI
 * PNGs and RLE/bitfields BMPs — so Coil's platform decoders keep handling those
 * and nothing regresses versus a stock setup. Animated results come back as
 * [KiteAnimationImage]; static ones as an ordinary bitmap image that
 * memory-caches normally.
 */
public class KiteImageDecoder(
    private val source: ImageSource,
    @Suppress("unused") private val options: Options,
) : Decoder {

    override suspend fun decode(): DecodeResult {
        val bytes = source.source().use { it.readByteArray() }
        val animation = KiteImage.decodeAnimation(bytes)
        val image = if (animation.isAnimated) {
            KiteAnimationImage(animation)
        } else {
            animation.frames.first().bitmap.toCoilImage(shareable = true)
        }
        return DecodeResult(image = image, isSampled = false)
    }

    public class Factory : Decoder.Factory {

        override fun create(
            result: SourceFetchResult,
            options: Options,
            imageLoader: ImageLoader,
        ): Decoder? {
            val peek = result.source.source().peek()
            peek.request(PEEK_BYTES.toLong())
            val header = peek.buffer.readByteArray(minOf(PEEK_BYTES.toLong(), peek.buffer.size))
            return if (claims(header)) KiteImageDecoder(result.source, options) else null
        }

        private fun claims(h: ByteArray): Boolean = when (ImageFormat.sniff(h)) {
            ImageFormat.GIF -> true
            ImageFormat.PNG -> pngIsDecodable(h)
            ImageFormat.BMP -> bmpIsDecodable(h)
            else -> false
        }

        /** Decline what PngDecoder would reject so Coil's platform path keeps it. */
        private fun pngIsDecodable(h: ByteArray): Boolean {
            if (h.size < 29) return false
            // First chunk type at offset 12; Apple's CgBI variant goes to the platform.
            if (h[12] == 'C'.code.toByte() && h[13] == 'g'.code.toByte() &&
                h[14] == 'B'.code.toByte() && h[15] == 'I'.code.toByte()
            ) return false
            // IHDR interlace flag (offset 28): Adam7 isn't decoded yet.
            return h[28].toInt() == 0
        }

        /** Decline what BmpDecoder would reject (compression, exotic depths, OS/2 header). */
        private fun bmpIsDecodable(h: ByteArray): Boolean {
            if (h.size < PEEK_BYTES) return false
            fun u16(at: Int) = (h[at].toInt() and 0xFF) or ((h[at + 1].toInt() and 0xFF) shl 8)
            fun u32(at: Int) = u16(at) or (u16(at + 2) shl 16)
            val dibSize = u32(14)
            if (dibSize != 40 && dibSize != 108 && dibSize != 124) return false
            val bpp = u16(28)
            if (bpp != 8 && bpp != 24 && bpp != 32) return false
            return u32(30) == 0   // BI_RGB only
        }

        private companion object {
            // Enough for the PNG IHDR interlace byte (29) and the BMP compression field (34).
            const val PEEK_BYTES = 34
        }
    }
}
