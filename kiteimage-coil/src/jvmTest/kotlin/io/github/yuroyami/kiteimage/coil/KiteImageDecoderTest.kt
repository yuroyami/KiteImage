package io.github.yuroyami.kiteimage.coil

import coil3.BitmapImage
import coil3.ImageLoader
import coil3.PlatformContext
import coil3.request.ImageRequest
import coil3.request.SuccessResult
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * End-to-end through a real Coil pipeline: ImageLoader → ByteArrayFetcher →
 * component registry → our decoder (or Coil's own fallback when we decline).
 */
class KiteImageDecoderTest {

    private fun hex(s: String): ByteArray =
        ByteArray(s.length / 2) { i -> ((s[i * 2].digitToInt(16) shl 4) or s[i * 2 + 1].digitToInt(16)).toByte() }

    // Vectors from gen_gif_vectors.py / gen_png_vectors.py (same as core commonTest).
    private val ANIM_KEEP_2F = "47494638396102000200910000ff000000ff000000ffffff0021ff0b4e45545343415045322e30030103000021f90404050000002c0000000002000200000204044110050021f90404000000002c000000000200020000020414455105003b"
    private val STATIC_GIF_3X2 = "47494638396103000200910000ff000000ff000000ffffff002c00000000030002000002050443710453003b"
    private val RGBA8_2X2_PNG = "89504e470d0a1a0a0000000d494844520000000200000002080600000072b60d24000000174944415478da63f8cfc0f01f081b1881b4c3ffff0c07003ee3077ce9dbb2f80000000049454e44ae426082"
    private val INTERLACED_PNG_1X1 = "89504e470d0a1a0a0000000d49484452000000010000000108000000014d79abc30000000a49444154789c636000000002000148afa4710000000049454e44ae426082"

    private val context: PlatformContext = PlatformContext.INSTANCE

    private fun loader(): ImageLoader = ImageLoader.Builder(context)
        .components { add(KiteImageDecoder.Factory()) }
        .build()

    private fun execute(bytes: ByteArray) = runBlocking {
        loader().execute(ImageRequest.Builder(context).data(bytes).build())
    }

    @Test
    fun animatedGifComesBackAsKiteAnimationImage() {
        val result = execute(hex(ANIM_KEEP_2F))
        val success = assertIs<SuccessResult>(result)
        val image = assertIs<KiteAnimationImage>(success.image)
        assertEquals(2, image.animation.frames.size)
        assertEquals(3, image.animation.loopCount)
        assertEquals(2, image.width)
        assertEquals(2, image.height)
        assertTrue(!image.shareable, "animations must skip the memory cache")
        // Frame pixels survived the whole pipeline.
        assertEquals(0xFFFF0000.toInt(), image.animation.frames[0].bitmap[0, 0])
        assertEquals(0xFF0000FF.toInt(), image.animation.frames[1].bitmap[0, 0])
    }

    @Test
    fun staticGifBecomesPlainBitmapImage() {
        val success = assertIs<SuccessResult>(execute(hex(STATIC_GIF_3X2)))
        // Single-frame GIF: no animation wrapper, normal shareable bitmap.
        val image = assertIs<BitmapImage>(success.image)
        assertEquals(3, image.width)
        assertTrue(image.shareable)
    }

    @Test
    fun pngDecodesThroughKiteImage() {
        val success = assertIs<SuccessResult>(execute(hex(RGBA8_2X2_PNG)))
        assertIs<BitmapImage>(success.image)
        assertEquals(2, success.image.width)
    }

    @Test
    fun interlacedPngDeclinedAndHandledByCoilsOwnDecoder() {
        // Our factory must return null → Coil falls through to SkiaImageDecoder,
        // which handles Adam7 fine. The request must still SUCCEED.
        val success = assertIs<SuccessResult>(execute(hex(INTERLACED_PNG_1X1)))
        assertTrue(success.image !is KiteAnimationImage)
        assertEquals(1, success.image.width)
    }

    @Test
    fun jpegFallsThroughToCoil() {
        val img = BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB)
        for (y in 0 until 4) for (x in 0 until 4) img.setRGB(x, y, 0xFF336699.toInt())
        val out = ByteArrayOutputStream()
        check(ImageIO.write(img, "jpg", out))

        val success = assertIs<SuccessResult>(execute(out.toByteArray()))
        assertTrue(success.image !is KiteAnimationImage)
        assertEquals(4, success.image.width)
    }
}
