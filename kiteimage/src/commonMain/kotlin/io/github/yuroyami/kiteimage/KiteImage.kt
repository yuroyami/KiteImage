package io.github.yuroyami.kiteimage

import io.github.yuroyami.kiteimage.codec.BmpDecoder
import io.github.yuroyami.kiteimage.codec.GifDecoder
import io.github.yuroyami.kiteimage.codec.PngDecoder

/**
 * The KiteImage facade. Everything is pure computation on byte arrays — no I/O,
 * no threads, no platform types — so it behaves identically on every KMP target.
 *
 * ```kotlin
 * val bitmap = KiteImage.decode(bytes)          // sniffs the format, dispatches
 * val anim = KiteImage.decodeAnimation(bytes)   // frames + delays + loop count
 * val format = KiteImage.detect(bytes)          // just the sniff
 * ```
 */
public object KiteImage {

    /** Identify [data]'s format from its magic bytes, or null if unrecognised. */
    public fun detect(data: ByteArray): ImageFormat? = ImageFormat.sniff(data)

    /**
     * Decode [data] into a [KiteBitmap], sniffing the format first. Animated
     * inputs yield their first frame (composited); use [decodeAnimation] for the
     * full sequence.
     *
     * @throws ImageDecodeException on malformed/truncated input or unknown format
     * @throws UnsupportedImageException on formats recognised but not yet decodable
     *   (see [ImageFormat] — sniffing is deliberately wider than decoding)
     */
    public fun decode(data: ByteArray): KiteBitmap = when (detect(data)) {
        ImageFormat.PNG -> PngDecoder.decode(data)
        ImageFormat.BMP -> BmpDecoder.decode(data)
        ImageFormat.GIF -> GifDecoder.decode(data, firstFrameOnly = true).frames.first().bitmap
        ImageFormat.JPEG -> throw UnsupportedImageException(
            "JPEG decoding is not in this version yet — it is on the roadmap",
        )
        ImageFormat.WEBP -> throw UnsupportedImageException(
            "WebP decoding is not in this version yet",
        )
        ImageFormat.TIFF -> throw UnsupportedImageException(
            "TIFF decoding is not in this version yet",
        )
        null -> throw ImageDecodeException(
            "unrecognised image format (${data.size} bytes${
                if (data.size >= 4) {
                    ", starts " + data.take(4).joinToString(" ") { b ->
                        (b.toInt() and 0xFF).toString(16).padStart(2, '0')
                    }
                } else ""
            })",
        )
    }

    /**
     * Decode [data] as an animation. GIF returns every composited frame with
     * delays and the loop count; static formats return a single zero-delay frame,
     * so this is safe to call on anything [decode] accepts.
     */
    public fun decodeAnimation(data: ByteArray): KiteAnimation = when (detect(data)) {
        ImageFormat.GIF -> GifDecoder.decode(data, firstFrameOnly = false)
        else -> {
            val single = decode(data)
            KiteAnimation(
                width = single.width,
                height = single.height,
                frames = listOf(KiteFrame(single, delayMillis = 0, delayRawCentiseconds = 0)),
                loopCount = 1,
            )
        }
    }
}
